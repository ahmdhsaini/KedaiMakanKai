package com.example.kedaimakankai.remote;

import com.example.kedaimakankai.model.Slot;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SlotService {

    @GET("slots/list_available")
    Call<List<Slot>> listAvailable();
}
