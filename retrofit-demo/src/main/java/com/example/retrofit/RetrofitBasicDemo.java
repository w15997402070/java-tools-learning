package com.example.retrofit;

import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Retrofit 2 基础演示
 *
 * 涵盖以下核心功能：
 * 1. Retrofit 实例创建与配置
 * 2. Service 接口定义（GET / POST / PUT / DELETE / Query / Path / Body）
 * 3. 同步调用（Call.execute()）
 * 4. 异步调用（Call.enqueue()）
 * 5. 使用 JSONPlaceholder 公开 API 进行真实演示
 *
 * 公开测试 API：https://jsonplaceholder.typicode.com
 */
public class RetrofitBasicDemo {

    // ==================== 数据模型 ====================

    /**
     * Post 数据模型
     * @SerializedName 用于字段名映射（JSON key -> Java field）
     */
    static class Post {
        @SerializedName("id")
        private int id;

        @SerializedName("userId")
        private int userId;

        @SerializedName("title")
        private String title;

        @SerializedName("body")
        private String body;

        public Post() {}

        public Post(int userId, String title, String body) {
            this.userId = userId;
            this.title = title;
            this.body = body;
        }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getTitle() { return title; }
        public String getBody() { return body; }

        @Override
        public String toString() {
            return "Post{id=" + id + ", userId=" + userId
                    + ", title='" + title + "', body='" + body + "'}";
        }
    }

    /**
     * Comment 数据模型
     */
    static class Comment {
        private int id;
        private int postId;
        private String name;
        private String email;
        private String body;

        public int getId() { return id; }
        public int getPostId() { return postId; }
        public String getName() { return name; }
        public String getEmail() { return email; }

        @Override
        public String toString() {
            return "Comment{id=" + id + ", postId=" + postId
                    + ", name='" + name + "', email='" + email + "'}";
        }
    }

    // ==================== Service 接口定义 ====================

    /**
     * JSONPlaceholder Posts API Service 接口
     *
     * Retrofit 通过注解描述 HTTP 接口，运行时动态生成实现。
     * 核心注解：
     *   @GET / @POST / @PUT / @DELETE / @PATCH  — HTTP 方法
     *   @Query       — URL 查询参数 (?key=value)
     *   @Path        — URL 路径参数 ({id})
     *   @Body        — 请求体（序列化为 JSON）
     *   @Field       — 表单参数（需配合 @FormUrlEncoded）
     *   @Header      — 单个请求头
     *   @Headers     — 多个静态请求头
     */
    interface PostApiService {

        /**
         * 获取所有 Post 列表
         * GET https://jsonplaceholder.typicode.com/posts
         */
        @GET("posts")
        Call<List<Post>> getAllPosts();

        /**
         * 根据 ID 获取单个 Post
         * GET https://jsonplaceholder.typicode.com/posts/{id}
         */
        @GET("posts/{id}")
        Call<Post> getPostById(@Path("id") int id);

        /**
         * 分页查询：Query 参数
         * GET https://jsonplaceholder.typicode.com/posts?userId=1
         */
        @GET("posts")
        Call<List<Post>> getPostsByUserId(@Query("userId") int userId);

        /**
         * 创建新 Post（POST + JSON Body）
         * POST https://jsonplaceholder.typicode.com/posts
         */
        @POST("posts")
        Call<Post> createPost(@Body Post post);

        /**
         * 更新 Post（PUT 全量替换）
         * PUT https://jsonplaceholder.typicode.com/posts/{id}
         */
        @PUT("posts/{id}")
        Call<Post> updatePost(@Path("id") int id, @Body Post post);

        /**
         * 删除 Post
         * DELETE https://jsonplaceholder.typicode.com/posts/{id}
         */
        @DELETE("posts/{id}")
        Call<Void> deletePost(@Path("id") int id);

        /**
         * 获取某篇 Post 下的评论（嵌套资源）
         * GET https://jsonplaceholder.typicode.com/posts/{postId}/comments
         */
        @GET("posts/{postId}/comments")
        Call<List<Comment>> getCommentsByPost(@Path("postId") int postId);
    }

    // ==================== 主程序 ====================

    public static void main(String[] args) throws InterruptedException {

        System.out.println("========== Retrofit 2 基础演示 ==========\n");

        // 1. 创建 Retrofit 实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")  // baseUrl 必须以 / 结尾
                .addConverterFactory(GsonConverterFactory.create()) // 使用 Gson 解析 JSON
                .build();

        // 2. 创建 Service 实例（动态代理）
        PostApiService service = retrofit.create(PostApiService.class);

        // --- 演示 1：同步 GET 列表（取前5条）---
        System.out.println("--- 演示1：同步 GET 获取所有 Post（取前5条）---");
        try {
            Call<List<Post>> call = service.getAllPosts();
            Response<List<Post>> response = call.execute(); // 同步执行（不可在主线程/Android UI线程调用）
            if (response.isSuccessful() && response.body() != null) {
                List<Post> posts = response.body();
                System.out.println("共获取 " + posts.size() + " 条 Post，展示前5条：");
                for (int i = 0; i < Math.min(5, posts.size()); i++) {
                    System.out.println("  " + posts.get(i));
                }
            } else {
                System.out.println("请求失败，HTTP " + response.code());
            }
        } catch (IOException e) {
            System.out.println("网络不可达（如需运行请检查网络）：" + e.getMessage());
        }

        // --- 演示 2：@Path 参数 GET 单条 ---
        System.out.println("\n--- 演示2：@Path 参数 GET 单条 Post ---");
        try {
            Response<Post> resp = service.getPostById(1).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                System.out.println("Post#1: " + resp.body());
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // --- 演示 3：@Query 参数过滤 ---
        System.out.println("\n--- 演示3：@Query 参数过滤（userId=1）---");
        try {
            Response<List<Post>> resp = service.getPostsByUserId(1).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                System.out.println("userId=1 的 Post 数量：" + resp.body().size());
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // --- 演示 4：POST 创建资源 ---
        System.out.println("\n--- 演示4：POST 创建新 Post（@Body）---");
        Post newPost = new Post(1, "Retrofit 学习日记", "今天学习了 Retrofit 2，非常好用！");
        try {
            Response<Post> resp = service.createPost(newPost).execute();
            if (resp.isSuccessful() && resp.body() != null) {
                System.out.println("创建成功，新 Post ID：" + resp.body().getId());
                System.out.println("  Title：" + resp.body().getTitle());
            }
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // --- 演示 5：DELETE 删除资源 ---
        System.out.println("\n--- 演示5：DELETE 删除 Post ---");
        try {
            Response<Void> resp = service.deletePost(1).execute();
            System.out.println("DELETE Post#1 HTTP状态：" + resp.code()
                    + (resp.code() == 200 ? " (成功)" : ""));
        } catch (IOException e) {
            System.out.println("网络不可达：" + e.getMessage());
        }

        // --- 演示 6：异步调用（enqueue）---
        System.out.println("\n--- 演示6：异步 GET（enqueue 回调）---");
        Call<List<Comment>> callAsync = service.getCommentsByPost(1);
        callAsync.enqueue(new retrofit2.Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call,
                                   Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    System.out.println("[异步回调] Post#1 下有 "
                            + response.body().size() + " 条评论");
                    if (!response.body().isEmpty()) {
                        System.out.println("  第一条评论：" + response.body().get(0));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                System.out.println("[异步回调] 失败：" + t.getMessage());
            }
        });

        // 等待异步回调执行
        Thread.sleep(3000);

        System.out.println("\n========== 基础演示结束 ==========");
    }
}
