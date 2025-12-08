package com.codepath.campgrounds

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.codepath.campgrounds.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import kotlinx.serialization.SerializationException

fun createJson() = Json {
    isLenient = true
    ignoreUnknownKeys = true
    useAlternativeNames = false
}

private const val TAG = "CampgroundsMain/"
private const val PARKS_API_KEY = BuildConfig.API_KEY
private const val CAMPGROUNDS_URL =
    "https://developer.nps.gov/api/v1/campgrounds?api_key=${PARKS_API_KEY}"

class MainActivity : AppCompatActivity() {
    private lateinit var campgroundsRecyclerView: RecyclerView
    private lateinit var binding: ActivityMainBinding
    private val campgrounds = mutableListOf<Campground>()
    private lateinit var campgroundAdapter: CampgroundAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        campgroundsRecyclerView = findViewById(R.id.campgrounds)

        campgroundAdapter = CampgroundAdapter(this, campgrounds)
        campgroundsRecyclerView.adapter = campgroundAdapter

        campgroundsRecyclerView.layoutManager = LinearLayoutManager(this).also {
            val dividerItemDecoration = DividerItemDecoration(this, it.orientation)
            campgroundsRecyclerView.addItemDecoration(dividerItemDecoration)
        }

        lifecycleScope.launch {
            (application as CampgroundApplication).db.campgroundDao().getAll().collect { databaseList ->
                val mappedList = databaseList.map { entity ->
                    Campground(
                        name = entity.name,
                        description = entity.description,
                        latLong = entity.latLong,
                        images = listOf(CampgroundImage(entity.imageUrl, ""))
                    )
                }
                campgrounds.clear()
                campgrounds.addAll(mappedList)
                campgroundAdapter.notifyDataSetChanged()
            }
        }

        val client = AsyncHttpClient()
        client.get(CAMPGROUNDS_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "Failed to fetch campgrounds: $statusCode")
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched campgrounds: $json")
                try {
                    val campgroundResponse = createJson().decodeFromString<CampgroundResponse>(
                        json.jsonObject.toString()
                    )
                    campgroundResponse.data?.let { list ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            (application as CampgroundApplication).db.campgroundDao().deleteAll()
                            (application as CampgroundApplication).db.campgroundDao().insertAll(
                                list.map { campground ->
                                    CampgroundEntity(
                                        name = campground.name,
                                        description = campground.description,
                                        latLong = campground.latLong,
                                        imageUrl = campground.imageUrl
                                    )
                                }
                            )
                        }
                    }
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to parse JSON: $e")
                }
            }

        })
    }
}
