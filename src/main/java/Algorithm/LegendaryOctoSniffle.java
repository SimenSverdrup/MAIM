package Algorithm;

import AIS.AIS;
import AIS.Antigen;
import AIS.Antibody;
import GUI.GUI;
import GUI.SolutionGraph;
import Island.IGA;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class LegendaryOctoSniffle extends Application {

    // private TextField populationSizeInput = new TextField();
    private boolean running = false;
    private GUI gui;
    private AIS ais;
    private ArrayList<AIS> allAIS;

    @Override
    public void start(Stage primaryStage) throws Exception {
        gui = new GUI(primaryStage, this);
    }

    public void run(int iterations, int populationSize, double mutationRate, int numberOfTournaments,
            String dataSetName, int labelIndex, double trainingTestSplit, double migrationFrequency,
            int numberOfIslands, int migrationRate) {
        this.running = true;
        gui.startButton.setDisable(true);
        gui.iterationTextField.setDisable(true);
        gui.stopButton.setDisable(false);

        DataSet dataSet = new DataSet("./DataSets/" + dataSetName, trainingTestSplit, labelIndex);

        IGA iga = new IGA(numberOfIslands, populationSize, iterations, migrationFrequency, migrationRate);
        iga.initialize(dataSet, mutationRate, numberOfTournaments, iterations);

        // this.ais = new
        // AIS(dataSet.trainingSet,dataSet.featureMap,dataSet.labels,dataSet.antigenMap,populationSize,
        // mutationRate,numberOfTournaments,iterations);
        this.ais = iga.getIsland(0).getAis(); // new
                                              // AIS(dataSet.trainingSet,dataSet.featureMap,dataSet.labels,dataSet.antigenMap,populationSize,
                                              // mutationRate,numberOfTournaments,iterations);
        this.allAIS = iga.getAllAIS();
        gui.createStatisticGraph(iterations);

        HashMap<String, ArrayList<Antigen>> testSetMap = Antigen.createAntigenMap(dataSet.testSet);
        ArrayList<HashMap<String, ArrayList<Antibody>>> antibodyGenerations = new ArrayList<>();
        Thread aisThread = new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                if (!this.getRunning()) {
                    break;
                }
                boolean migrate = iga.migrate();
                int it = 0;
                for (AIS _ais : allAIS) {
                    it++;
                    _ais.setIteration(_ais.getIteration() + 1);
                    // antibodyGenerations.add(AIS.copy(ais.getAntibodyMap()));
                    double accuracy = AIS.vote(_ais.getAntigenMap(), _ais.getAntibodyMap());
                    // System.out.println("Island: " + it + ": Acc="+accuracy);
                    // if(iga.getNumberOfIslands() == it){
                    // it = 0;
                    // System.out.println("----- Iteration " + i + "-----");
                    // }
                    double accuracyTestSet = AIS.vote(testSetMap, ais.getAntibodyMap());
                    // gui.addIteration(accuracy);
                }

                // ais.setIteration(ais.getIteration()+1);
                antibodyGenerations.add(AIS.copy(ais.getAntibodyMap()));
                double accuracy = AIS.vote(ais.getAntigenMap(), ais.getAntibodyMap());
                double accuracyTestSet = AIS.vote(testSetMap, ais.getAntibodyMap());
                gui.addIteration(accuracy, migrate);

                if (accuracy > ais.getBestAccuracy()) {
                    gui.setBestAccuracy(accuracy);
                    ais.setBestAccuracy(accuracy);
                    ais.setBestItreation(i);
                }
                if (accuracyTestSet > ais.getBestAccuracyTestSet()) {
                    ais.setBestAccuracyTestSet(accuracyTestSet);
                    ais.setBestIterationTestSet(i);
                }
                ais.iterate();

                for (int j = 1; j < allAIS.size(); j++) {
                    allAIS.get(j).iterate();
                }
            }

            antibodyGenerations.add(ais.getAntibodyMap());

            // double accuracy = AIS.vote(testSetMap, ais.getAntibodyMap());
            Platform.runLater(() -> {
                gui.startButton.setDisable(false);
                gui.startButton.requestFocus();
                gui.iterationTextField.setDisable(false);
                gui.stopButton.setDisable(true);
                this.gui.setAntibodyGenerations(antibodyGenerations, ais.getAntigenMap(), testSetMap,
                        antibodyGenerations.get(ais.getBestItreation()));
                this.gui.createSolutionGraph(ais.getFeatureMap(), ais.getAntibodyMap());
                gui.drawSolution(testSetMap, antibodyGenerations.get(ais.getBestItreation()));
                gui.setBestAccuracyIteration(ais.getBestAccuracy(), ais.getBestItreation());
            });
        });
        aisThread.start();
    }

    public synchronized boolean getRunning() {
        return running;
    }

    public void stopRunning() {
        running = false;
        gui.startButton.setDisable(false);
        gui.stopButton.setDisable(true);
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public GUI getGui() {
        return gui;
    }

    public void setGui(GUI gui) {
        this.gui = gui;
    }

    public AIS getAis() {
        return ais;
    }

    public void setAis(AIS ais) {
        this.ais = ais;
    }
}