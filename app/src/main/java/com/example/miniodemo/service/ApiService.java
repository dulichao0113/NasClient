package com.example.miniodemo.service;

import com.example.miniodemo.model.BaseResponse;
import com.example.miniodemo.model.DeleteRequest;
import com.example.miniodemo.model.Folder;
import com.example.miniodemo.model.Image;
import com.example.miniodemo.model.ImageListResponse;
import com.example.miniodemo.model.LoginRequest;
import com.example.miniodemo.model.LoginResponse;
import com.example.miniodemo.model.MultiUploadResponse;
import com.example.miniodemo.model.RegisterRequest;
import com.example.miniodemo.model.RegisterResponse;
import com.example.miniodemo.model.UploadResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // 注册接口
    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    // 登录接口
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

//    // 单图上传接口（原有）
//    @Multipart
//    @POST("images/upload")
//    Call<UploadResponse> uploadSingleImage(
//            @Header("Authorization") String token,
//            @Part MultipartBody.Part image,
//            @Part("fileName") RequestBody fileName
//    );

    @Multipart
    @POST("images/upload")
    Call<UploadResponse> uploadSingleImage(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image,
            @Part("fileName") RequestBody fileName,
            @Part("folderId") RequestBody folderId // 新增参数
    );

    // 多图上传接口
    @Multipart
    @POST("images/upload-multiple")
    Call<MultiUploadResponse> uploadMultipleImages(
            @Header("Authorization") String token,
            @Part List<MultipartBody.Part> images
    );

//    // 图片列表接口（全用户可见）
//    @GET("images")
//    Call<List<Image>> getImages(@Header("Authorization") String token);
    // 修改获取图片列表接口，添加参数
    @GET("images")
    Call<List<Image>> getImages(
            @Header("Authorization") String token,
            @Query("folderId") String folderId,
            @Query("isFavorite") boolean isFavorite
    );

    // 单张图片接口（全用户可见）
    @GET("images/{id}")
    Call<Image> getImage(
            @Header("Authorization") String token,
            @Path("id") int id
    );

    // 批量删除接口（修正后）
    @POST("images/batch-delete") // 改为POST方法
    Call<BaseResponse> deleteImages(
            @Header("Authorization") String token,
            @Body DeleteRequest deleteRequest
    );

    // 添加文件夹相关接口
    @GET("folders")
    Call<List<Folder>> getFolders(@Header("Authorization") String token);

    @POST("folders/ensure-all-images")
    Call<BaseResponse> ensureAllImagesFolder(@Header("Authorization") String token);
}

