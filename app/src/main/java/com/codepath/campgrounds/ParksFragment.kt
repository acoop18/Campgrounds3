package com.codepath.campgrounds

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import okhttp3.Headers

private const val TAG = "ParksFragment"
private const val API_KEY = BuildConfig.API_KEY
private const val PARKS_URL =
    "https://developer.nps.gov/api/v1/parks?api_key=${API_KEY}"

class ParksFragment : Fragment() {

    private val parks = mutableListOf<Park>()
    private lateinit var parksRecyclerView: RecyclerView
    private lateinit var parksAdapter: ParksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_parks, container, false)

        val layoutManager = LinearLayoutManager(context)
        parksRecyclerView = view.findViewById(R.id.parks)
        parksRecyclerView.layoutManager = layoutManager
        parksRecyclerView.setHasFixedSize(true)
        parksAdapter = ParksAdapter(view.context, parks)
        parksRecyclerView.adapter = parksAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchParks()
    }

    private fun fetchParks() {
        val client = AsyncHttpClient()
        client.get(PARKS_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "Failed to fetch parks. Status code: $statusCode, Response: $response", throwable)
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched parks: $json")
                lifecycleScope.launch {
                    try {
                        // Move JSON parsing and filtering to a background thread
                        val parksWithImages = withContext(Dispatchers.Default) {
                            val parsedJson = createJson().decodeFromString<ParksResponse>(
                                json.jsonObject.toString()
                            )
                            val list = parsedJson.data ?: emptyList()
                            Log.d(TAG, "Total parks fetched from API: ${list.size}")
                            val filteredList = list.filter { park -> park.images?.isNotEmpty() == true }
                            Log.d(TAG, "Parks with images after filtering: ${filteredList.size}")
                            filteredList
                        }

                        // Update the UI on the main thread
                        parks.clear()
                        parks.addAll(parksWithImages)
                        parksAdapter.notifyDataSetChanged()

                    } catch (e: SerializationException) {
                        Log.e(TAG, "Exception: $e")
                    }
                }
            }
        })
    }

    companion object {
        fun newInstance(): ParksFragment {
            return ParksFragment()
        }
    }
}
