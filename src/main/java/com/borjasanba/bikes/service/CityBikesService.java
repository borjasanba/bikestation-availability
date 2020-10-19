package com.borjasanba.bikes.service;


import com.borjasanba.bikes.model.StationStat;
import com.borjasanba.bikes.model.Station;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.SendTo;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@EnableBinding(CityBikesServiceProcessor.class)
public class CityBikesService {

    @NonNull
    private ObjectMapper mapper;

    @Value("${bike.nyc.availability.threshold:10}")
    private Integer availabilityThreshold;

    @StreamListener
    @SendTo("lowAvailability")
    public KStream<String, String> process(@Input("stationStatus") KStream<String, String> stationStatus,
                                           @Input("stationInfoTable") KTable<String, String> stationInfo) {
        return stationStatus.map((stationId, stationStr) -> new KeyValue<>(stationId, Integer.toString(getStation(stationStr).getNumBikesAvailable())))
                .leftJoin(stationInfo.mapValues(stationStr -> getStation(stationStr)), (numBikes, info) -> StationStat.builder()
                        .availabilityRatio(100 * Integer.parseInt(numBikes) / (float) Optional.ofNullable(info.getCapacity()).orElse(1))
                        .latitude(info.getLatitude())
                        .longitude(info.getLongitude())
                        .name(info.getName())
                        .build())
                .filter((stationId, stats) -> stats.getAvailabilityRatio() < availabilityThreshold)
                .map((stationId, stats) -> new KeyValue<>(stationId, Try.of(() -> mapper.writeValueAsString(stats)).get()))
                .peek((stationId, stats) -> log.info("low availability station: {} {}", stationId, stats));
    }

    private Station getStation(final String stationStr) {
        return Try.of(() -> mapper.readValue(stationStr, Station.class))
                .onFailure(e -> log.error("deserialize error {}", e))
                .get();
    }

}

interface CityBikesServiceProcessor {

    @Input("stationStatus")
    KStream<String, String> input();

    @Input("stationInfoTable")
    KTable<String, String> inputKTable();

    @Output("lowAvailability")
    KStream<String, String> output();

}