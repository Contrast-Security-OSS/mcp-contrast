package com.contrast.labs.ai.mcp.contrast.tool.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class RouteMapperPropertyTest {

  private final RouteMapper mapper = new RouteMapper();

  @Property
  void coveragePercent_should_always_be_in_zero_to_hundred(
      @ForAll @IntRange(min = 0, max = 50) int exercisedCount,
      @ForAll @IntRange(min = 0, max = 50) int discoveredCount) {
    var response = responseWith(exercisedCount, discoveredCount);

    var result = mapper.toResponseLight(response);

    assertThat(result.coveragePercent()).isBetween(0.0, 100.0);
  }

  @Property
  void coveragePercent_should_be_zero_when_no_routes_exist() {
    var response = new RouteCoverageResponse();
    response.setRoutes(List.of());

    var result = mapper.toResponseLight(response);

    assertThat(result.coveragePercent()).isEqualTo(0.0);
    assertThat(result.totalRoutes()).isZero();
  }

  @Property
  void coveragePercent_should_have_at_most_two_decimal_places(
      @ForAll @IntRange(min = 0, max = 50) int exercisedCount,
      @ForAll @IntRange(min = 0, max = 50) int discoveredCount) {
    var response = responseWith(exercisedCount, discoveredCount);

    var result = mapper.toResponseLight(response);

    var shifted = result.coveragePercent() * 100.0;
    assertThat(shifted)
        .as("coverage %s should have at most 2 decimal places", result.coveragePercent())
        .isCloseTo(Math.round(shifted), within(1e-9));
  }

  @Property
  void coveragePercent_should_be_hundred_when_all_exercised(
      @ForAll @IntRange(min = 1, max = 50) int count) {
    var response = responseWith(count, 0);

    var result = mapper.toResponseLight(response);

    assertThat(result.coveragePercent()).isEqualTo(100.0);
    assertThat(result.exercisedCount()).isEqualTo(count);
  }

  @Property
  void coveragePercent_should_be_zero_when_none_exercised(
      @ForAll @IntRange(min = 1, max = 50) int count) {
    var response = responseWith(0, count);

    var result = mapper.toResponseLight(response);

    assertThat(result.coveragePercent()).isEqualTo(0.0);
    assertThat(result.discoveredCount()).isEqualTo(count);
  }

  @Property
  void totalRoutes_should_equal_exercised_plus_discovered(
      @ForAll @IntRange(min = 0, max = 50) int exercisedCount,
      @ForAll @IntRange(min = 0, max = 50) int discoveredCount) {
    var response = responseWith(exercisedCount, discoveredCount);

    var result = mapper.toResponseLight(response);

    assertThat(result.totalRoutes()).isEqualTo(exercisedCount + discoveredCount);
  }

  @Property
  void null_routes_should_yield_zero_coverage() {
    var response = new RouteCoverageResponse();
    response.setRoutes(null);

    var result = mapper.toResponseLight(response);

    assertThat(result.coveragePercent()).isEqualTo(0.0);
    assertThat(result.totalRoutes()).isZero();
    assertThat(result.routes()).isEmpty();
  }

  @Property
  void authoritative_counts_should_override_computed_counts(
      @ForAll @IntRange(min = 1, max = 20) int exercisedCount,
      @ForAll @IntRange(min = 1, max = 20) int discoveredCount) {
    var routes = new ArrayList<Route>();
    for (var i = 0; i < exercisedCount + discoveredCount; i++) {
      var route = new Route();
      route.setSignature("sig" + i);
      routes.add(route);
    }
    var response = new RouteCoverageResponse();
    response.setRoutes(routes);
    response.setCount(exercisedCount + discoveredCount);
    response.setExercisedCount(exercisedCount);
    response.setDiscoveredCount(discoveredCount);

    var result = mapper.toResponseLight(response);

    assertThat(result.exercisedCount()).isEqualTo(exercisedCount);
    assertThat(result.discoveredCount()).isEqualTo(discoveredCount);
    assertThat(result.coveragePercent()).isBetween(0.0, 100.0);
  }

  private static RouteCoverageResponse responseWith(int exercisedCount, int discoveredCount) {
    var routes = new ArrayList<Route>();
    for (var i = 0; i < exercisedCount; i++) {
      var route = new Route();
      route.setSignature("exercised-" + i);
      route.setStatus("EXERCISED");
      routes.add(route);
    }
    for (var i = 0; i < discoveredCount; i++) {
      var route = new Route();
      route.setSignature("discovered-" + i);
      route.setStatus("DISCOVERED");
      routes.add(route);
    }
    var response = new RouteCoverageResponse();
    response.setRoutes(routes);
    return response;
  }
}
