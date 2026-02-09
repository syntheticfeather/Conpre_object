package com.example.loanapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

data class LoginRequest(
    val phone: String,
    val password: String
)

data class LoginResponse(
    val token: String
)

data class ApiResponse<T>(
    val code: Int,
    val data: T,
    val message: String
)

interface ApiService {
    @POST("/api/auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>
}

class LoginActivity : AppCompatActivity() {

    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 初始化UI组件
        phoneEditText = findViewById(R.id.edit_phone)
        passwordEditText = findViewById(R.id.edit_password)
        loginButton = findViewById(R.id.btn_login)
        progressBar = findViewById(R.id.progress_bar)

        // 初始化SharedPreferences
        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // 检查是否已登录
        checkIfLoggedIn()

        // 设置登录按钮点击监听
        loginButton.setOnClickListener { attemptLogin() }
    }

    private fun checkIfLoggedIn() {
        val token = sharedPreferences.getString("auth_token", null)
        if (!token.isNullOrEmpty()) {
            // 已登录，直接跳转到主界面
            navigateToMain()
        }
    }

    private fun attemptLogin() {
        val phone = phoneEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (phone.isEmpty() || password.isEmpty()) {
            showToast("请输入手机号和密码")
            return
        }

        showLoading(true)

        // 创建API服务
        val apiService = createApiService()

        // 创建登录请求
        val request = LoginRequest(phone, password)

        // 调用登录API
        apiService.login(request).enqueue(object : Callback<ApiResponse<LoginResponse>> {
            override fun onResponse(
                call: Call<ApiResponse<LoginResponse>>,
                response: Response<ApiResponse<LoginResponse>>
            ) {
                showLoading(false)
                if (response.isSuccessful) {
                    response.body()?.let { apiResponse ->
                        if (apiResponse.code == 200) {
                            // 保存token
                            saveToken(apiResponse.data.token)
                            // 跳转到主界面
                            navigateToMain()
                        } else {
                            showToast(apiResponse.message ?: "登录失败")
                        }
                    } ?: run {
                        showToast("响应数据为空")
                    }
                } else {
                    showToast("服务器错误: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<LoginResponse>>, t: Throwable) {
                showLoading(false)
                showToast("网络请求失败: ${t.message}")
            }
        })
    }

    private fun createApiService(): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://your-server-address/") // 替换为实际服务器地址
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        loginButton.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}