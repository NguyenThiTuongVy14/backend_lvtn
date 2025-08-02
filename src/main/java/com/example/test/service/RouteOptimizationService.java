package com.example.test.service;

import com.example.test.entity.JobPosition;
import com.example.test.entity.JobRotationTemp;
import com.example.test.entity.Route;
import com.example.test.entity.Vehicle;
import com.example.test.repository.JobPositionRepository;
import com.example.test.repository.RouteRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.Position;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RouteOptimizationService {

    @Autowired
    private ApiService apiService;
    @Autowired
    private JobPositionRepository jobPositionRepository;
    @Autowired
    private RouteRepository routeRepository;

    public Map<String, Object> optimizeRoute(List<JobRotationTemp> jobRotationTempList, Vehicle vehicle) {
        String url = "https://api.openrouteservice.org/optimization";
        String token = "5b3ce3597851110001cf6248fed2cd4609bf4466add139b1d39b785d";

        JSONObject body = new JSONObject();

        JSONArray jobs = new JSONArray();
        for (JobRotationTemp jobRotationTemp : jobRotationTempList) {
            Optional<JobPosition> position = jobPositionRepository.findById(jobRotationTemp.getJobPositionId());
            if(position.isPresent()) {
                Double lat = position.get().getLat().doubleValue();
                Double lng = position.get().getLng().doubleValue();

                jobs.put(new JSONObject()
                        .put("id", jobRotationTemp.getId())
                        .put("location", new JSONArray(new double[]{lng, lat})));
            }
        }

        JSONArray vehicles = new JSONArray();
        vehicles.put(new JSONObject()
                .put("id", vehicle.getId())
                .put("start", new JSONArray(new double[]{106.700, 10.776}))
                .put("end", new JSONArray(new double[]{106.800, 10.870}))
                .put("profile", "driving-car"));

        body.put("jobs", jobs);
        body.put("vehicles", vehicles);
        System.out.println(body.toString(2)); // in đẹp dạng JSON format


        return apiService.postData(url, body, token);
    }

    public void convertAndSaveRoute(Map<String, Object> map) {
        List<Map<String, Object>> routes = (List<Map<String, Object>>) map.get("routes");

        for (Map<String, Object> route : routes) {
            Integer vehicle = (Integer) route.get("vehicle");
//            Integer cost = (Integer) route.get("cost");

            List<Map<String, Object>> steps = (List<Map<String, Object>>) route.get("steps");
            Integer index = 1;
            for (Map<String, Object> step : steps) {
                String type = (String) step.get("type");
                Integer idJob = (Integer) step.get("id");
                Integer arrival = (Integer) step.get("arrival");

                Route newRoute = new Route();
                newRoute.setVehicleId(vehicle);
                newRoute.setRotationId(idJob);
                newRoute.setIndex(index++);
                newRoute.setType(type);
                newRoute.setArrival(arrival);
                routeRepository.save(newRoute);
            }
        }
    }
}
