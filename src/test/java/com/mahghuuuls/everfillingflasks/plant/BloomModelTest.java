package com.mahghuuuls.everfillingflasks.plant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each bloom's click and outline box reaches the top of its model, so the outline never cuts
 * through the plant. A redrawn model that grows taller fails here instead of in game.
 */
class BloomModelTest {

    private static double modelTop(WildPlant plant) {
        String path = "assets/everfillingflasks/models/block/" + plant.key() + "_bloom.json";
        InputStream stream = BloomModelTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " must be on the classpath");
        JsonObject model = new JsonParser().parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        // The listed top of each part, before rotation: right for parts turned about y, and for the
        // small parts tilted about x or z today; a large tilted part could reach higher than this.
        double top = 0.0D;
        for (JsonElement element : model.getAsJsonArray("elements")) {
            top = Math.max(top, element.getAsJsonObject().getAsJsonArray("to").get(1).getAsDouble());
        }
        return top / 16.0D;
    }

    @Test
    void everyBloomBoxReachesTheTopOfItsModel() {
        for (WildPlant plant : WildPlant.values()) {
            double top = modelTop(plant);
            assertTrue(plant.bloomTop() >= top, plant + ": box top " + plant.bloomTop() + " below model top " + top);
            assertTrue(plant.bloomTop() <= 1.0D, plant + ": box top above the block");
        }
    }
}
