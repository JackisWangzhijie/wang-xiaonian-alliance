package com.longcheer.cockpit.nav.sdk.amap

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.ServiceSettings
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.poisearch.PoiResultV2
import com.amap.api.services.poisearch.PoiSearchV2
import com.longcheer.cockpit.nav.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高德搜索适配器
 * 封装高德搜索SDK功能
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class AMapSearchAdapter @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AMapSearchAdapter"
        private const val SEARCH_PAGE_SIZE = 20
    }
    
    private val geocodeSearch: GeocodeSearch by lazy {
        GeocodeSearch(context.applicationContext)
    }
    
    private val inputTips: Inputtips by lazy {
        Inputtips(context.applicationContext)
    }
    
    /**
     * 搜索POI
     */
    suspend fun searchPoi(param: PoiSearchParam): List<Poi> = 
        suspendCancellableCoroutine { continuation ->
            try {
                val query = PoiSearchV2.Query(param.keyword, param.category?.name ?: "", param.city)
                query.pageSize = param.pageSize
                query.pageNum = param.page - 1
                
                val poiSearch = PoiSearchV2(context.applicationContext, query)
                
                param.location?.let {
                    poiSearch.bound = PoiSearchV2.SearchBound(
                        LatLonPoint(it.latitude, it.longitude),
                        param.radius
                    )
                }
                
                poiSearch.setOnPoiSearchListener(object : PoiSearchV2.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResultV2?, errorCode: Int) {
                        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                            val pois = result?.pois?.map { it.toPoiModel() } ?: emptyList()
                            continuation.resume(pois)
                        } else {
                            continuation.resumeWithException(
                                Exception("POI搜索失败: $errorCode")
                            )
                        }
                    }
                    
                    override fun onPoiItemSearched(poiItem: com.amap.api.services.core.PoiItemV2?, errorCode: Int) {}
                })
                
                poiSearch.searchPOIAsyn()
                
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    
    /**
     * 地理编码（地址转坐标）
     */
    suspend fun geocode(address: String, city: String? = null): LatLng? =
        suspendCancellableCoroutine { continuation ->
            val query = GeocodeQuery(address, city ?: "")
            
            geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(result: com.amap.api.services.geocoder.GeocodeResult?, errorCode: Int) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                        val location = result?.geocodeAddressList?.firstOrNull()?.latLonPoint
                        continuation.resume(location?.let { LatLng(it.latitude, it.longitude) })
                    } else {
                        continuation.resumeWithException(Exception("地理编码失败: $errorCode"))
                    }
                }
                
                override fun onRegeocodeSearched(result: com.amap.api.services.geocoder.RegeocodeResult?, errorCode: Int) {}
            })
            
            geocodeSearch.getFromLocationNameAsyn(query)
        }
    
    /**
     * 逆地理编码（坐标转地址）
     */
    suspend fun reverseGeocode(latLng: LatLng, radius: Float = 200f): String? =
        suspendCancellableCoroutine { continuation ->
            val query = RegeocodeQuery(
                LatLonPoint(latLng.latitude, latLng.longitude),
                radius.toInt(),
                GeocodeSearch.AMAP
            )
            
            geocodeSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onGeocodeSearched(result: com.amap.api.services.geocoder.GeocodeResult?, errorCode: Int) {}
                
                override fun onRegeocodeSearched(result: com.amap.api.services.geocoder.RegeocodeResult?, errorCode: Int) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                        val address = result?.regeocodeAddress?.formatAddress
                        continuation.resume(address)
                    } else {
                        continuation.resumeWithException(Exception("逆地理编码失败: $errorCode"))
                    }
                }
            })
            
            geocodeSearch.getFromLocationAsyn(query)
        }
    
    /**
     * 获取输入提示
     */
    suspend fun getInputTips(keyword: String, city: String? = null): List<String> =
        suspendCancellableCoroutine { continuation ->
            val query = InputtipsQuery(keyword, city)
            inputTips.query = query
            
            inputTips.setInputtipsListener { tipList, errorCode ->
                if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                    val tips = tipList?.map { it.name } ?: emptyList()
                    continuation.resume(tips)
                } else {
                    continuation.resumeWithException(Exception("输入提示失败: $errorCode"))
                }
            }
            
            inputTips.requestInputtipsAsyn()
        }
    
    /**
     * 扩展函数：POI实体转换
     */
    private fun com.amap.api.services.core.PoiItemV2.toPoiModel(): Poi {
        return Poi(
            id = poiId ?: "",
            name = title ?: "",
            type = parsePoiType(typeCode ?: ""),
            location = LatLng(latLonPoint?.latitude ?: 0.0, latLonPoint?.longitude ?: 0.0),
            address = snippet ?: "",
            phone = tel,
            distance = distance,
            rating = 0f, // 高德V2版本POI可能不包含评分
            price = 0,
            businessHours = null,
            photos = emptyList(),
            isOpen = null
        )
    }
    
    private fun parsePoiType(typeCode: String): PoiType {
        return when {
            typeCode.startsWith("01") -> PoiType.GAS_STATION
            typeCode.startsWith("01") -> PoiType.CHARGING_STATION
            typeCode.startsWith("05") -> PoiType.RESTAURANT
            typeCode.startsWith("10") -> PoiType.HOTEL
            typeCode.startsWith("15") -> PoiType.PARKING
            typeCode.startsWith("17") -> PoiType.SCENIC
            typeCode.startsWith("09") -> PoiType.SHOPPING
            else -> PoiType.OTHER
        }
    }
}