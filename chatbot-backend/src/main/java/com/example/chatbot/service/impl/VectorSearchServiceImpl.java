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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {
    private final MilvusServiceClient milvusClient;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RedisService redisService;

    private static final String KNOWLEDGE_DATA_KEY = "knowledge_data:";
    private static final String COLLECTION_NAME = "knowledge_base";
    private static final String VECTOR_FIELD = "vector";
    private static final String ID_FIELD = "id";
    private static final int VECTOR_DIM = 384; // MiniLM-L6-v2 dimension

    // 嵌入服务地址
    private static final String EMBEDDING_URL = "http://localhost:8888/embed";

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
                            .withDimension(VECTOR_DIM)
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
                    .withExtraParam("{\"M\": 8, \"efConstruction\": 64}")
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
    private List<Float> generateEmbedding(String text) throws Exception {
        Map<String, String> request = Map.of("text", text);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(EMBEDDING_URL, entity, String.class);

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
                    .build();

            R<SearchResults> resp = milvusClient.search(searchParam);
            if (resp.getStatus() != R.Status.Success.getCode()) {
                log.error("Milvus search failed: {}", resp.getMessage());
                return new ArrayList<>();
            }
            SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
            List<Long> ids = new ArrayList<>();
            for (SearchResultsWrapper.IDScore idScore : wrapper.getIDScore(0)) {
                ids.add(idScore.getLongID());
            }
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
                    
                    // 将新查询到的文档存入Redis
                    for (KnowledgeBase doc : dbResults) {
                        redisService.saveDocToRedis(doc);
                    }
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