from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModel
import torch

app = FastAPI()

# 加载 HuggingFace 模型
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModel.from_pretrained(MODEL_NAME)

class EmbeddingRequest(BaseModel):
    text: str

@app.post("/embed")
async def embed(req: EmbeddingRequest):
    inputs = tokenizer(req.text, return_tensors="pt", truncation=True, max_length=512)
    with torch.no_grad():
        model_output = model(**inputs)
    # 取 [CLS] token 的输出
    embeddings = model_output.last_hidden_state[:, 0, :].squeeze().tolist()
    return {"embedding": embeddings}