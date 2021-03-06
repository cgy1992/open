package com.mapzen.open.route;

import com.mapzen.open.support.MapzenTestRunner;
import com.mapzen.osrm.Instruction;
import com.mapzen.osrm.Route;

import org.fest.assertions.data.Offset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.location.Location;

import static com.mapzen.open.support.TestHelper.MOCK_ACE_HOTEL;
import static com.mapzen.open.support.TestHelper.getTestLocation;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(MapzenTestRunner.class)
public class RouteEngineTest {
    private RouteEngine routeEngine;
    private Route route;
    private TestRouteListener listener;

    @Before
    public void setUp() throws Exception {
        route = new Route(MOCK_ACE_HOTEL);
        listener = new TestRouteListener();
        routeEngine = new RouteEngine();
        routeEngine.setRoute(route);
        routeEngine.setListener(listener);
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(routeEngine).isNotNull();
    }

    @Test
    public void onRecalculate_shouldNotifyWhenLost() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(getTestLocation(0, 0));
        assertThat(listener.recalculating).isTrue();
    }

    @Test
    public void onSnapLocation_shouldReturnCorrectedLocation() throws Exception {
        Location location = getTestLocation(40.7444114, -73.9904202);
        routeEngine.onLocationChanged(location);
        assertThat(listener.originalLocation).isEqualsToByComparingFields(location);
        assertThat(listener.snapLocation).isEqualsToByComparingFields(route.snapToRoute(location));
    }

    @Test
    public void onApproachInstruction_shouldReturnIndex() throws Exception {
        Location start = route.getRouteInstructions().get(0).getLocation();
        routeEngine.onLocationChanged(start);
        Location preLoc = getTestLocation(40.743486, -73.988273);
        routeEngine.onLocationChanged(preLoc);
        Location loc = route.getRouteInstructions().get(1).getLocation();
        routeEngine.onLocationChanged(loc);
        assertThat(listener.approachIndex).isEqualTo(1);
    }

    @Test
    public void onApproachInstruction_shouldNotFireForDestination() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(5).getLocation());
        assertThat(listener.approachIndex).isNotEqualTo(5);
    }

    @Test
    public void onApproachInstruction_shouldFireAtStart() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat(listener.approachIndex).isEqualTo(0);
    }

    @Test
    public void onInstructionComplete_shouldReturnIndex() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        assertThat(listener.completeIndex).isEqualTo(1);
    }

    @Test
    public void onUpdateDistance_shouldReturnDistanceToNextInstruction() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat((double) listener.distanceToNextInstruction)
                .isEqualTo(route.getRouteInstructions().get(0).getDistance(), Offset.offset(1.0));
    }

    @Test
    public void onUpdateDistance_shouldHaveFullDistanceToDestinationAtStart() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat((double) listener.distanceToDestination)
                .isEqualTo((double) route.getTotalDistance(), Offset.offset(1.0));
    }

    @Test
    public void onUpdateDistance_shouldHaveZeroDistanceToNextInstructionAtStart() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        assertThat((double) listener.distanceToNextInstruction)
                .isEqualTo(route.getRouteInstructions().get(0).getDistance(), Offset.offset(1.0));
    }

    @Test
    public void onUpdateDistance_shouldCountdownDistanceToDestinationAtTurn() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        assertThat((double) listener.distanceToDestination).isEqualTo((double)
                (route.getTotalDistance() - route.getRouteInstructions().get(0).getDistance()),
                Offset.offset(1.0));
    }

    @Test
    public void onUpdateDistance_shouldCountdownInstructionDistance() throws Exception {
        Location location = getTestLocation(40.743810, -73.989053); // 26th & Broadway
        routeEngine.onLocationChanged(route.getRouteInstructions().get(0).getLocation());
        routeEngine.onLocationChanged(location);

        int expected = route.getDistanceToNextInstruction();
        assertThat(listener.distanceToNextInstruction).isEqualTo(expected);
    }

    @Test
    public void onUpdateDistance_shouldCountdownDistanceToDestinationAlongRoute() throws Exception {
        Instruction instruction = route.getRouteInstructions().get(0);
        Location location = getTestLocation(40.743810, -73.989053); // 26th & Broadway
        routeEngine.onLocationChanged(instruction.getLocation());
        routeEngine.onLocationChanged(location);

        Location snapLocation = route.snapToRoute(location);
        Location nextInstruction = route.getRouteInstructions().get(1).getLocation();
        int distanceToNextInstruction = (int) snapLocation.distanceTo(nextInstruction);
        int expected = route.getTotalDistance() - instruction.getDistance()
                + distanceToNextInstruction;
        assertThat((double) listener.distanceToDestination).isEqualTo(expected, Offset.offset(2.0));
    }

    @Test
    public void onUpdateDistance_shouldFireWhenLost() throws Exception {
        routeEngine.onLocationChanged(getTestLocation(0, 0));
        assertThat((double) listener.distanceToDestination)
                .isEqualTo(route.getTotalDistance(), Offset.offset(1.0));
    }

    @Test
    public void onUpdateDistance_shouldReturnZeroAtDestination() throws Exception {
        int size = route.getRouteInstructions().size();
        routeEngine.onLocationChanged(route.getRouteInstructions().get(size - 1).getLocation());
        assertThat(listener.distanceToNextInstruction).isEqualTo(0);
        assertThat(listener.distanceToDestination).isEqualTo(0);
    }

    @Test
    public void onRouteComplete_shouldTriggerAtDestination() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(5).getLocation());
        assertThat(listener.routeComplete).isTrue();
    }

    @Test
    public void onLocationChange_shouldNotBeLostIfItNeverSnapped() throws Exception {
        Location loc = getTestLocation(0, 0);
        routeEngine.onLocationChanged(loc);
        assertThat(listener.recalculating).isFalse();
    }

    @Test
    public void onRouteComplete_shouldOnlyTriggerOnce() throws Exception {
        routeEngine.onLocationChanged(route.getRouteInstructions().get(1).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(2).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(3).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(4).getLocation());
        routeEngine.onLocationChanged(route.getRouteInstructions().get(5).getLocation());
        listener.routeComplete = false;
        routeEngine.onLocationChanged(route.getRouteInstructions().get(5).getLocation());
        assertThat(listener.routeComplete).isFalse();
    }

    private static class TestRouteListener implements RouteEngine.RouteListener {
        private Location originalLocation;
        private Location snapLocation;

        private boolean recalculating = false;
        private int approachIndex = -1;
        private int completeIndex = -1;
        private int distanceToNextInstruction = -1;
        private int distanceToDestination = -1;
        private boolean routeComplete = false;

        @Override
        public void onRecalculate(Location location) {
            recalculating = true;
        }

        @Override
        public void onSnapLocation(Location originalLocation, Location snapLocation) {
            this.originalLocation = originalLocation;
            this.snapLocation = snapLocation;
        }

        @Override
        public void onApproachInstruction(int index) {
            approachIndex = index;
        }

        @Override
        public void onInstructionComplete(int index) {
            completeIndex = index;
        }

        @Override
        public void onUpdateDistance(int distanceToNextInstruction, int distanceToDestination) {
            this.distanceToNextInstruction = distanceToNextInstruction;
            this.distanceToDestination = distanceToDestination;
        }

        @Override
        public void onRouteComplete() {
            routeComplete = true;
        }
    }
}
