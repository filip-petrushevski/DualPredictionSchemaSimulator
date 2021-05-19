package dual_prediction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dual_prediction.PredictingAlgorithm.LINEAR_PREDICTOR;

public class OneDimensionalDualPredictionSchemaSimulator {
    // Specified parameters
    private final String fileName = "measurements.csv";
    private final double temperatureThreshold = 10;
    private final PredictingAlgorithm predictingAlgorithm = LINEAR_PREDICTOR;

    // Calculated parameters
    private Double temperaturesRootMeanSquareError;
    private Double fractionOfMeasurementsSent;

    private final List<Double> temperatures = new ArrayList<>();
    private final List<Double> receivedTemperatures = new ArrayList<>();
    private Integer numOfMeasurements;

    public static void main(String[] args) throws IOException {
        new OneDimensionalDualPredictionSchemaSimulator().simulateAndPrintResults();
    }

    private void simulateAndPrintResults() throws IOException {
        readMeasurements();
        calculateMeasurementsReceived();
        calculateRootMeanSquareError();
        printResults();
    }

    private void readMeasurements() throws IOException {
        try (var reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine(); // skip first line
            String line;
            while ((line = reader.readLine()) != null) {
                String[] lineValues = line.split(",");
                double temperature = Double.parseDouble(lineValues[0]);
                temperatures.add(temperature);
            }
        }
        numOfMeasurements = temperatures.size();
    }

    private void calculateMeasurementsReceived() {
        Integer numOfMeasurementsSent = 1;
        Double lastReceivedTemperature = temperatures.get(0);
        Double secondLastReceivedTemperature = null;
        receivedTemperatures.add(lastReceivedTemperature);

        for (int i = 1; i < numOfMeasurements; i++) {
            double currTemperature = temperatures.get(i);
            double predictedTemperature = predictValue(lastReceivedTemperature, secondLastReceivedTemperature);

            if (differenceIsAboveThreshold(currTemperature, predictedTemperature, temperatureThreshold)) {
                numOfMeasurementsSent++;
                lastReceivedTemperature = currTemperature;
            } else {
                lastReceivedTemperature = predictedTemperature;
            }
            receivedTemperatures.add(lastReceivedTemperature);
            secondLastReceivedTemperature = receivedTemperatures.get(i - 1);
        }
        fractionOfMeasurementsSent = ((double) numOfMeasurementsSent) / numOfMeasurements;
    }

    private boolean differenceIsAboveThreshold(Double val1, Double val2, Double threshold) {
        return Math.abs(val1 - val2) > threshold;
    }

    private Double predictValue(Double lastReceivedTemperature, Double secondLastReceivedTemperature) {
        switch (predictingAlgorithm) {
            case LINEAR_PREDICTOR:
                return lastReceivedTemperature;
            case MOVING_AVERAGE:
                if (secondLastReceivedTemperature == null) {
                    return lastReceivedTemperature;
                }
                return (lastReceivedTemperature + secondLastReceivedTemperature) / 2;
            case WEIGHTED_MOVING_AVERAGE:
                if (secondLastReceivedTemperature == null) {
                    return lastReceivedTemperature;
                }
                return (lastReceivedTemperature * 0.75 + secondLastReceivedTemperature * 0.25);
            default:
                throw new RuntimeException("Predicting algorithm not supported");
        }
    }

    private void calculateRootMeanSquareError() {
        temperaturesRootMeanSquareError = Math.pow(
                Stream.iterate(0, i -> i + 1)
                        .limit(numOfMeasurements)
                        .mapToDouble(i -> squaredDifference(receivedTemperatures.get(i), temperatures.get(i)))
                        .average().getAsDouble(),
                0.5);
    }

    private double squaredDifference(Double val1, Double val2) {
        return Math.pow(val1 - val2, 2);
    }

    private void printResults() {
        System.out.println("temperaturesRootMeanSquareError = " + temperaturesRootMeanSquareError);
        System.out.println("fractionOfMeasurementsSent = " + fractionOfMeasurementsSent);
    }
}
