package com.example.chatbot.service.impl;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
import com.example.chatbot.service.RedisService;
import com.example.chatbot.service.VectorSearchService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.chatbot.service.RedisDistributedLock;
import com.example.chatbot.exception.BusinessException;
import com.example.chatbot.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {
    private final MilvusServiceClient milvusClient;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RedisService redisService;
    private final RedisDistributedLock distributedLock;

    private static final String KNOWLEDGE_DATA_KEY = "knowledge_data:";
    private static final String COLLECTION_NAME = "knowledge_base";
    private static final String VECTOR_FIELD = "vector";
    private static final String ID_FIELD = "id";

    @Value("${embedding.url}")
    private String embeddingUrl;
    @Value("${embedding.vector-dim:384}")
    private int vectorDim;
    @Value("${milvus.search.nprobe:50}")
    private int nprobe;
    @Value("${milvus.search.score-threshold:0.9}")
    private double scoreThreshold;
    @Value("${milvus.index.m:8}")
    private int hnswM;
    @Value("${milvus.index.ef-construction:64}")
    private int hnswEfConstruction;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化方法：创建 Milvus 集合
     */
    public void init() {
        createCollection();
    }

    /**
     * 创建 Milvus 集合和索引（如不存在）
     */
    private void createCollection() {
        try {
            R<Boolean> hasCollectionResp = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .build());
            if (Boolean.TRUE.equals(hasCollectionResp.getData())) {
                return;
            }

            List<FieldType> fieldTypes = List.of(
                    FieldType.newBuilder()
                            .withName(ID_FIELD)
                            .withDataType(io.milvus.grpc.DataType.Int64)
                            .withPrimaryKey(true)
                            .withAutoID(false)
                            .build(),
                    FieldType.newBuilder()
                            .withName(VECTOR_FIELD)
                            .withDataType(io.milvus.grpc.DataType.FloatVector)
                            .withDimension(vectorDim)
                            .build()
            );
            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFieldTypes(fieldTypes)
                    .build();

            milvusClient.createCollection(createCollectionParam);

            CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFieldName(VECTOR_FIELD)
                    .withIndexType(IndexType.HNSW)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam(String.format("{\"M\": %d, \"efConstruction\": %d}", hnswM, hnswEfConstruction))
                    .build();

            milvusClient.createIndex(createIndexParam);

            log.info("Successfully created Milvus collection: {}", COLLECTION_NAME);
        } catch (Exception e) {
            log.error("Failed to create Milvus collection", e);
            throw new RuntimeException("Failed to create Milvus collection", e);
        }
    }

    /**
     * 通过 Python 服务生成文本向量
     */
    List<Float> generateEmbedding(String text) throws Exception {
        Map<String, String> request = Map.of("text", text);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(embeddingUrl, entity, String.class);

        Map<String, Object> result = objectMapper.readValue(response.getBody(), Map.class);
        List<Double> embeddingDouble = (List<Double>) result.get("embedding");
        List<Float> embedding = new ArrayList<>();
        for (Double d : embeddingDouble) {
            embedding.add(d.floatValue());
        }
        return embedding;
    }

    /**
     * 检索相似文档
     * nprobe越大搜索更广，匹配更精准，但查询速度会稍慢
     * score(0-1):代表返回结果的相似度,
     */
    @Override
    public List<KnowledgeBase> searchSimilar(String query, int topK) {
        try {
            List<Float> queryVector = generateEmbedding(query);

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withVectorFieldName(VECTOR_FIELD)
                    .withVectors(List.of(queryVector))
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withOutFields(List.of(ID_FIELD))
                    .withParams(Map.of("nprobe", String.valueOf(nprobe)).toString())
                    .build();

            R<SearchResults> resp = milvusClient.search(searchParam);
            if (resp.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus search failed: {}", resp.getMessage());
                return new ArrayList<>();
            }

            // 过滤相似度分数低于阈值的结果
            SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
            List<Long> ids = wrapper.getIDScore(0).stream()
                    .filter(idScore -> idScore.getScore() > scoreThreshold)
                    .map(SearchResultsWrapper.IDScore::getLongID)
                    .collect(Collectors.toList());

            if (!ids.isEmpty()) {
                // 先从Redis缓存中查找
                List<KnowledgeBase> results = new ArrayList<>();
                List<Long> missingIds = new ArrayList<>();
                
                for (Long id : ids) {
                    KnowledgeBase cachedDoc = (KnowledgeBase) redisService.getRedisTemplate()
                            .opsForValue().get(KNOWLEDGE_DATA_KEY + id);
                    if (cachedDoc != null) {
                        results.add(cachedDoc);
                    } else {
                        missingIds.add(id);
                    }
                }

                // 如果Redis中没有找到所有文档，则从数据库中查询缺失的文档
                if (!missingIds.isEmpty()) {
                    List<KnowledgeBase> dbResults = knowledgeBaseMapper.findByIds(missingIds);
                    results.addAll(dbResults);
                }
                
                return results;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to search similar documents", e);
            throw new RuntimeException("Failed to search similar documents", e);
        }
    }

    /**
     * 单条文档入库（向量化并存入 Milvus）
     */
    @Override
    public void indexDocument(KnowledgeBase knowledge) {
        String lockKey = "vector:index:" + knowledge.getId();
        String lockValue = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        try {
            if (lockValue != null) {
                try {
                    List<Float> vector = generateEmbedding(knowledge.getTitle() + " " + knowledge.getContent());

                    List<InsertParam.Field> fields = new ArrayList<>();
                    fields.add(new InsertParam.Field(ID_FIELD, List.of(knowledge.getId())));
                    fields.add(new InsertParam.Field(VECTOR_FIELD, List.of(vector)));

                    InsertParam insertParam = InsertParam.newBuilder()
                            .withCollectionName(COLLECTION_NAME)
                            .withFields(fields)
                            .build();

                    milvusClient.insert(insertParam);
                    log.debug("Successfully indexed document: {}", knowledge.getTitle());
                } catch (Exception e) {
                    log.error("Failed to index document", e);
                    throw new RuntimeException("Failed to index document", e);
                }
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } finally {
            if (lockValue != null) {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 批量文档入库
     */
    @Override
    public void indexDocuments(List<KnowledgeBase> knowledgeList) {
        try {
            List<Long> ids = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();
            for (KnowledgeBase knowledge : knowledgeList) {
                ids.add(knowledge.getId());
                vectors.add(generateEmbedding(knowledge.getTitle() + " " + knowledge.getContent()));
            }

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(ID_FIELD, ids));
            fields.add(new InsertParam.Field(VECTOR_FIELD, vectors));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
            log.info("Successfully indexed {} documents", knowledgeList.size());
        } catch (Exception e) {
            log.error("Failed to index documents", e);
            throw new RuntimeException("Failed to index documents", e);
        }
    }

    /**
     * 删除文档向量
     */
    @Override
    public void deleteDocument(Long id) {
        String lockKey = "vector:delete:" + id;
        String lockValue = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
        try {
            if (lockValue != null) {
                try {
                    milvusClient.delete(io.milvus.param.dml.DeleteParam.newBuilder()
                            .withCollectionName(COLLECTION_NAME)
                            .withExpr(ID_FIELD + " == " + id)
                            .build());
                    log.debug("Successfully deleted document index: {}", id);
                } catch (Exception e) {
                    log.error("Failed to delete document index", e);
                    throw new RuntimeException("Failed to delete document index", e);
                }
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } finally {
            if (lockValue != null) {
                distributedLock.unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 更新文档向量（先删后加）
     */
    @Override
    public void updateDocument(KnowledgeBase knowledge) {
        try {
            deleteDocument(knowledge.getId());
            indexDocument(knowledge);
            log.debug("Successfully updated document index: {}", knowledge.getTitle());
        } catch (Exception e) {
            log.error("Failed to update document index", e);
            throw new RuntimeException("Failed to update document index", e);
        }
    }
} 