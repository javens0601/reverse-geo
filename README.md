## 提供逆地理编码查询功能 

🔥 服务启动
```shell
java -jar build/libs/reverse-geo-0.0.1-SNAPSHOT.jar
```

🔥 批量逆地理编码查询
```shell
curl -X POST --location "http://localhost:8080/api/geocode/batch-reverse" \
    -H "Content-Type: application/json" \
    -d '[
          "106.465,26.740",
          "116.413,39.860",
          "116.396,39.985"
        ]'
```
