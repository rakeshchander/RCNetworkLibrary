package io.github.rcnetworklibrary

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.native.concurrent.ThreadLocal

interface RequestInterceptor {
    fun getRequestHeaders() : Map<String, String> = HashMap()
    fun updateRequestBody(requestBody: JsonElement) : JsonElement = requestBody
}

interface ResponseInterceptor {
    fun updateResponseBody(responseBody: String) : String = responseBody
}

typealias TokenHandler = ((success: (Unit)->Unit, error: (Throwable) -> Unit) -> Unit)

interface APIManager {
    fun getCompletePath(contextPath: String) : String
    fun getRequestInterceptors(contextPath: String) : List<RequestInterceptor>
    fun getResponseInterceptors(contextPath: String) : List<ResponseInterceptor>
    fun getNetworkDispatcher(contextPath: String) : NetworkDispatcher = CoreNetworkClient()
    fun getTokenHandler(contextPath: String) : TokenHandler?
}

class NetworkManager {
    @ThreadLocal
    companion object {
        lateinit var apiManager: APIManager
    }

}

class NetworkConstants {

    companion object {
        val invalidRequest : Int = -34124
        val invalidResponse : Int = -32234
    }

}

/**
 * Network Dispatcher Interface for API Consumption
 * Implement this interface to define specific implementation for Network Calls - to be made by SDK
 */
interface NetworkDispatcher {

    fun consumeRequest(request: HttpRequestBuilder, onSuccess: (String, Int) -> Unit, onError: (Throwable) -> Unit, tokenHandler: TokenHandler?)
}

class CoreNetworkClient : NetworkDispatcher {

     private val client: HttpClient
        get() = HttpClient(){
            expectSuccess = false
        }

    override fun consumeRequest(
        request: HttpRequestBuilder,
        onSuccess: (String, Int) -> Unit,
        onError: (Throwable) -> Unit,
        tokenHandler: TokenHandler?
    ) {

        GlobalScope.launch(ApplicationDispatcher) {

            try {
                val result : HttpResponse = client.request(request)
                if (result.status.value == 401 && tokenHandler != null) {
                    tokenHandler({
                         consumeRequest(request, onSuccess, onError, tokenHandler)
                    },{
                        onError(it)
                    })
                } else {
                    onSuccess(result.receive(), result.status.value)
                }

            } catch (ex: Exception) {
                onError(ex)
            }

            client.close()

        }

    }
}

/**
 * GenericError Error Class to handle any error / exception scenario
 */
class GenericError(error: Throwable?, val errorCode: Int) {

    /**
     * Error Message for the caught error / exception
     */
    lateinit var errorMessage: String
    /**
     * Error Stacktrace for the caught error / exception
     */
    lateinit var stackTrace: String

    /**
     * Constructor for initializing
     */
    init {
        if (error != null) {
            errorMessage = error.message ?: ""
            stackTrace = error.stackTraceToString()
        }
    }
}