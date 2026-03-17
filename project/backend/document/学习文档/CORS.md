# CORS配置

```java
// personal-loan/src/main/java/com/example/demo/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 此行配置可以通过的跨域请求的来源，如果后续有新的请求来源，加后面
                // 在哪看？打开html的时候，看他的网址。
                .allowedOrigins("http://127.0.0.1:5500", "null", "http://localhost:63342/")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

## 由周飞凤配置

