package io.github.rcnetworklibrary

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

class APIClient(
    val contextPath: String
) {

    val jsonParser = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    inline fun<reified R, reified E> consumeGETRequest(crossinline onSuccess: (@Serializable R) -> Unit,
                                                       crossinline onErrorResponse: (@kotlinx.serialization.Serializable E) -> Unit,
                                                       crossinline onError: (GenericError) -> Unit) {

        val requestBuilder = prepareRequest<String>()
        requestBuilder.method = HttpMethod.Get

        NetworkManager.apiManager.getNetworkDispatcher(contextPath).consumeRequest(requestBuilder, onSuccess = { response, statusCode ->

            handleResponse(Pair(response, statusCode), onSuccess, onErrorResponse, onError)

        }, onError = {
            onError(GenericError(error = it, NetworkConstants.invalidResponse))
        }, tokenHandler = NetworkManager.apiManager.getTokenHandler(contextPath))
    }

    inline fun<reified T, reified R, reified E> consumePOSTRequest(requestBody: @kotlinx.serialization.Serializable T,
                                                                   crossinline onSuccess: (@kotlinx.serialization.Serializable R) -> Unit,
                                                                   crossinline onErrorResponse: (@kotlinx.serialization.Serializable E) -> Unit,
                                                                   crossinline onError: (GenericError) -> Unit) {

        val requestBuilder = prepareRequest(requestBody)
        requestBuilder.method = HttpMethod.Post

        NetworkManager.apiManager.getNetworkDispatcher(contextPath).consumeRequest(requestBuilder, onSuccess = { response, statusCode ->

            handleResponse(Pair(response, statusCode), onSuccess, onErrorResponse, onError)

        }, onError = {
            onError(GenericError(error = it, NetworkConstants.invalidResponse))
        }, tokenHandler = NetworkManager.apiManager.getTokenHandler(contextPath))
    }

    inline fun<reified T, reified R, reified E> consumePUTRequest(requestBody: @kotlinx.serialization.Serializable T?,
                                                                  crossinline onSuccess: (@kotlinx.serialization.Serializable R) -> Unit,
                                                                  crossinline onErrorResponse: (@kotlinx.serialization.Serializable E) -> Unit,
                                                                  crossinline onError: (GenericError) -> Unit) {

        val requestBuilder = prepareRequest(requestBody)
        requestBuilder.method = HttpMethod.Put

        NetworkManager.apiManager.getNetworkDispatcher(contextPath).consumeRequest(requestBuilder, onSuccess = { response, statusCode ->

            handleResponse(Pair(response, statusCode), onSuccess, onErrorResponse, onError)

        }, onError = {
            onError(GenericError(error = it, NetworkConstants.invalidResponse))
        }, tokenHandler = NetworkManager.apiManager.getTokenHandler(contextPath))
    }

    inline fun<reified T, reified R, reified E> consumeDELETERequest(requestBody: @kotlinx.serialization.Serializable T?,
                                                                     crossinline onSuccess: (@kotlinx.serialization.Serializable R) -> Unit,
                                                                     crossinline onErrorResponse: (@kotlinx.serialization.Serializable E) -> Unit,
                                                                     crossinline onError: (GenericError) -> Unit) {

        val requestBuilder = prepareRequest(requestBody)
        requestBuilder.method = HttpMethod.Delete

        NetworkManager.apiManager.getNetworkDispatcher(contextPath).consumeRequest(requestBuilder, onSuccess = { response, statusCode ->

            handleResponse(Pair(response, statusCode), onSuccess, onErrorResponse, onError)

        }, onError = {
            onError(GenericError(error = it, NetworkConstants.invalidResponse))
        }, tokenHandler = NetworkManager.apiManager.getTokenHandler(contextPath))
    }

    inline fun<reified R, reified E> handleResponse(response: Pair<String, Int>,
                                                    crossinline onSuccess: (@kotlinx.serialization.Serializable R) -> Unit,
                                                    crossinline onErrorResponse: (@kotlinx.serialization.Serializable E) -> Unit,
                                                    crossinline onError: (GenericError) -> Unit){
        var responseBody = response.first
        for (item in NetworkManager.apiManager.getResponseInterceptors(contextPath)) {
            responseBody = item.updateResponseBody(responseBody)
        }

        try {

            val finalResponse : R = jsonParser.decodeFromString(responseBody)
            onSuccess(finalResponse)

        } catch (error: Throwable) {

            try {
                val errorResponse : E = jsonParser.decodeFromString(responseBody)
                onErrorResponse(errorResponse)
            } catch (err: Throwable) {
                onError(GenericError(error = error, NetworkConstants.invalidResponse))
            }

        }
    }

    inline fun<reified T> prepareRequest(requestBody: @kotlinx.serialization.Serializable T? = null) : HttpRequestBuilder {

        val requestBuilder = HttpRequestBuilder(){
            URLBuilder(NetworkManager.apiManager.getCompletePath(contextPath))
        }

        for (item in NetworkManager.apiManager.getRequestInterceptors(contextPath)){
            val headerMap = item.getRequestHeaders()
            for (header in headerMap.keys) {
                requestBuilder.header(header, headerMap[header])
            }
        }

        var body : JsonElement = jsonParser.encodeToJsonElement(requestBody)
        for (item in NetworkManager.apiManager.getRequestInterceptors(contextPath)){
            body = item.updateRequestBody(body)
        }
        requestBuilder.body = body

        return requestBuilder
    }

}