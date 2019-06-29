import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MonteCarloSimulation {
    static Random rand = new Random();
    static double investmentAmount = 100000;
    static double years = 20;
    static double inflation = 3.5;
    static int interation = 10000;

    private static class Portfolio {
        double meanReturn;
        double risk;
        String name;

        Portfolio(String name, double meanReturn, double risk) {
            this.meanReturn = meanReturn;
            this.risk = risk;
            this.name = name;
        }
    }

    private static class Result {
        double median;
        double bestCase;
        double worstCase;
        String name;

        Result(String name, double median, double bestCase, double worstCase) {
            this.median = median;
            this.bestCase = bestCase;
            this.worstCase = worstCase;
            this.name = name;
        }
    }

    private static class Simulation implements Callable<Result> {
        Portfolio portfolio;

        Simulation(Portfolio portfolio) {
            this.portfolio = portfolio;
        }

        @Override
        public Result call() {
            double[] returnAmountList = new double[interation];
            for (int i = 0; i < interation; i++) {
                double wwrMultiplier = 1;
                double returnAmount = investmentAmount;
                for (int j = 0; j < years; j++) {
                    double rateOfReturn = rand.nextGaussian() * portfolio.risk + portfolio.meanReturn;
                    double inflationAdjustedRateOfReturn = ((1 + (rateOfReturn / 100)) / (1 + (inflation / 100)) - 1);
                    returnAmount = returnAmount * (1 + inflationAdjustedRateOfReturn);
                    wwrMultiplier = wwrMultiplier * (1 + rateOfReturn / 100);
                }
                returnAmountList[i] = returnAmount;
            }
            Arrays.sort(returnAmountList);
            double medianReturn = percentile(returnAmountList, 50);
            double bestCase = percentile(returnAmountList, 90);
            double worstCase = percentile(returnAmountList, 10);

            Result result = new Result(portfolio.name, medianReturn, bestCase, worstCase);

            return result;
        }


        public static double percentile(double[] returnAmountList, double percentile) {
            if (percentile == 0) {
                return returnAmountList[0];
            }
            if (percentile == 100) {
                return returnAmountList[returnAmountList.length - 1];
            }
            if (percentile > 100) {
                throw new IllegalStateException("Invalid value for percentile");
            }
            if (returnAmountList.length == 1) {
                return returnAmountList[0];
            }

            double index = ((percentile * returnAmountList.length) / 100);

            double median = Math.floor(index);

            if (median == index) {
                return returnAmountList[(int) index];
            } else {
                return (returnAmountList[(int) index] + returnAmountList[(int) index - 1]) / 2;
            }
        }


    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        Portfolio aggressibePortfolio = new Portfolio("Aggressive", 9.4324, 15.675);
        Portfolio veryConservativePortfolio = new Portfolio("Very Conservative", 6.189, 6.3438);


        List<Portfolio> portfolios = new ArrayList<>();
        portfolios.add(aggressibePortfolio);
        portfolios.add(veryConservativePortfolio);

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            List<Simulation> simulations = new ArrayList<>();

            for (Portfolio portfolio : portfolios) {
                Simulation simulation = new Simulation(portfolio);
                simulations.add(simulation);
            }

            List<Future<Result>> futrueResultList = executor.invokeAll(simulations, 2000, TimeUnit.MILLISECONDS);

            printResult(futrueResultList);

            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("Total Time -> " + totalTime);

        } finally {
            executor.shutdown();
        }

    }


    private static void printResult(List<Future<Result>> futrueResultList) throws ExecutionException, InterruptedException {
        System.out.format("+----------------------+-----------------+-----------------+-----------------+%n");
        System.out.format("| Column name          | Median 20th Year| 10 %% Best Case  | 10 %% Worst Case |%n");
        System.out.format("+----------------------+-----------------+-----------------+-----------------+%n");
        String leftAlignFormat = "| %-20s | %-15f | %-15f | %-15f |%n";

        for (Future<Result> resultFuture : futrueResultList) {
            Result result = resultFuture.get();
            System.out.format(leftAlignFormat, result.name, result.median, result.bestCase, result.worstCase);
        }
        System.out.format("+----------------------+-----------------+-----------------+-----------------+%n");
    }
}
