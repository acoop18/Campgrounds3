package com.codepath.campgrounds

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.codepath.campgrounds.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.Headers

private const val TAG = "MainActivity"
private const val API_KEY = BuildConfig.API_KEY
private const val CAMPGROUNDS_URL =
    "https://developer.nps.gov/api/v1/campgrounds?api_key=${API_KEY}"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            lateinit var fragment: Fragment
            when (item.itemId) {
                R.id.action_parks -> fragment = ParksFragment()
                R.id.action_search -> fragment = CampgroundsFragment()
            }
            replaceFragment(fragment)
            true
        }

        binding.bottomNavigation.selectedItemId = R.id.action_parks

        fetchCampgrounds()
    }

    private fun fetchCampgrounds() {
        val client = AsyncHttpClient()
        client.get(CAMPGROUNDS_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "Failed to fetch campgrounds. Status code: $statusCode, Response: $response", throwable)
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched campgrounds: $json")
                try {
                    val parsedJson = createJson().decodeFromString<CampgroundResponse>(
                        json.jsonObject.toString()
                    )
                    parsedJson.data?.let { list ->
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
                    Log.e(TAG, "Exception: $e")
                }
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.main_frame_layout, fragment)
        fragmentTransaction.commit()
    }
}
