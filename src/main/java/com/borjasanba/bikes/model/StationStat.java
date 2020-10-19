package com.borjasanba.bikes.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StationStat {

    private String name;
    private Float availabilityRatio;
    private Double latitude;
    private Double longitude;

}
