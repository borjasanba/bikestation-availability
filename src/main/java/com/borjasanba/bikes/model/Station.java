package com.borjasanba.bikes.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Station {

    @JsonProperty("station_id")
    private String stationId;
    private String name;
    @JsonProperty("lat")
    private Double latitude;
    @JsonProperty("lon")
    private Double longitude;
    private Integer capacity;
    @JsonProperty("num_bikes_available")
    private Integer numBikesAvailable;

}
