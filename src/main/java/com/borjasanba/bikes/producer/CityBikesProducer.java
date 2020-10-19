package com.borjasanba.bikes.producer;

import com.borjasanba.bikes.model.Station;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityBikesProducer {

    private static final String STATION_INFORMATION_TOPIC = "station_information";
    private static final String STATION_STATUS_TOPIC = "station_status";

    @NonNull
    private KafkaTemplate<String, String> kafkaTemplate;

    @NonNull
    private ObjectMapper mapper;

    public void sendStationInfo(final Station stationInfo) {
        Try.of(() -> mapper.writeValueAsString(stationInfo))
                .onSuccess(value -> kafkaTemplate.send(STATION_INFORMATION_TOPIC, stationInfo.getStationId(), value))
                .onFailure(e -> log.error("error serializing stationInfo {}", e));
    }

    public void sendStationStatus(final Station stationStatus) {
        Try.of(() -> mapper.writeValueAsString(stationStatus))
                .onSuccess(value -> kafkaTemplate.send(STATION_STATUS_TOPIC, stationStatus.getStationId(), value))
                .onFailure(e -> log.error("error serializing stationInfo {}", e));
    }

}
