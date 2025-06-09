package com.example.chatbot.service.impl;

import ai.djl.inference.Predictor;
import ai.djl.modality.Input;
import ai.djl.modality.Output;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.mapper.KnowledgeBaseMapper;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {
    private final MilvusServiceClient milvusClient;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    
    @Value("${vector.model.name:sentence-transformers/all-MiniLM-L6-v2}")
    private String modelName;
    
    private static final String COLLECTION_NAME = "knowledge_base";
    private static final String VECTOR_FIELD = "vector";
    private static final String ID_FIELD = "id";
    private static final int VECTOR_DIM = 384; // MiniLM-L6-v2 dimension
    
    private ZooModel<Input, Output> model;
    
    public void init() {
        try {
            Criteria<Input, Output> criteria = Criteria.builder()
                    .setTypes(Input.class, Output.class)
                    .optModelUrls(modelName) // 推荐用 optModelUrls，modelName 可为本地路径或 HuggingFace URL
                    .optProgress(new ProgressBar())
                    .build();
            
            model = criteria.loadModel();
            createCollection();
        } catch (Exception e) {
            log.error("Failed to initialize vector search service", e);
            throw new RuntimeException("Failed to initialize vector search service", e);
        }
    }
    
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

    @Override
    public List<KnowledgeBase> searchSimilar(String query, int topK) {
        try (Predictor<Input, Output> predictor = model.newPredictor()) {
            // 生成查询向量
            List<Float> queryVector = generateEmbedding(query, predictor);
            
            // 搜索相似向量
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
                return knowledgeBaseMapper.findByIds(ids);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to search similar documents", e);
            throw new RuntimeException("Failed to search similar documents", e);
        }
    }

    @Override
    public void indexDocument(KnowledgeBase knowledge) {
        try (Predictor<Input, Output> predictor = model.newPredictor()) {
            // 生成文档向量
            List<Float> vector = generateEmbedding(knowledge.getTitle() + " " + knowledge.getContent(), predictor);
            
            // 存储到Milvus
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

    @Override
    public void indexDocuments(List<KnowledgeBase> knowledgeList) {
        try (Predictor<Input, Output> predictor = model.newPredictor()) {
            List<Long> ids = new ArrayList<>();
            List<List<Float>> vectors = new ArrayList<>();
            
            for (KnowledgeBase knowledge : knowledgeList) {
                ids.add(knowledge.getId());
                vectors.add(generateEmbedding(knowledge.getTitle() + " " + knowledge.getContent(), predictor));
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

    @Override
    public void updateDocument(KnowledgeBase knowledge) {
        try {
            // 删除旧索引
            deleteDocument(knowledge.getId());
            // 创建新索引
            indexDocument(knowledge);
            log.debug("Successfully updated document index: {}", knowledge.getTitle());
        } catch (Exception e) {
            log.error("Failed to update document index", e);
            throw new RuntimeException("Failed to update document index", e);
        }
    }

    private List<Float> generateEmbedding(String text, Predictor<Input, Output> predictor) throws Exception {
        Input input = new Input();
        input.add(text);
        Output output = predictor.predict(input);
        NDList ndList = (NDList) output.get(0);
        float[] arr = ndList.singletonOrThrow().toFloatArray();
        List<Float> embedding = new ArrayList<>();
        for (float v : arr) {
            embedding.add(v);
        }
        return embedding;
    }
} 