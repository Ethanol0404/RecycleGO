package my.edu.utar.RecycleGO.utils;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface RecycleApiService {

    @GET("recycle_tips.json")
    Call<List<RecycleTip>> getRecyclingTips();

    // Data model for the REST response
    class RecycleTip {
        private int id;
        private String title;
        private String body;

        public String getTitle() { return title; }
        public String getBody() { return body; }
    }
}
