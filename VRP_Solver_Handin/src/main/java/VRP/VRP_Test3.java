package VRP;

import jadex.commons.Tuple2;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.MaxRegret;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import jade.core.*;
import jade.wrapper.*;
import VRP.messaging.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

// this is the solver that is used by the rest of the program
public class VRP_Test3 {

    static SolverInterface _interface;

    // object that acts as test data
    public static class parameters
    {
        // number of supplier locations, including the depot
        public int suppliers;

        // number of delivery agents
        public int vehicles;

        // vehicles capacity limit (assumes all vehicles have the same capacity)
        public int vehicleCapacity;

        // capacity limit for each vehicle
        public int[] vehicleCapacities = new int[]{};

        // the ordered capacity of each location (12 locations, not including depot)
        public int[] supplierCapacity = new int[]{};

        // indexed supplier capacity
        public Tuple2<Integer, Integer>[] indSupCap = new Tuple2[]{};

        // creates the indexed supplier capacity array
        /*public Tuple2<Integer, Integer>[] formSupplierCapacity(int[] capacity)
        {
            Tuple2<Integer, Integer>[] array = new Tuple2[capacity.length];

            for (int i = 0; i < capacity.length; i++)
            {
                array[i] = new Tuple2<>(i + 1, capacity[i]);
            }

            return array;
        }*/

        public void formLocationCapacity() {
            Tuple2<Integer, Integer>[] array = new Tuple2[supplierCapacity.length];

            for (int i = 0; i < supplierCapacity.length; i++)
            {
                array[i] = new Tuple2<>(i + 1, supplierCapacity[i]);
            }

            indSupCap = array;
        }


        // capacity of the depot
        public int depotCapacity = 0;

        // coordinates of the locations
        public int[][] coordinates = new int[][]{};

        // each locations distance from each other location,
        // ordered, and includes the depot
        public int[][] distances = new int[][]{};

        // maximum distance between two locations in the distance matrix
        /*public int max = Arrays.stream(distances)
                .flatMapToInt(Arrays::stream).max()
                .orElse(1);*/
    }

    // global object that contains the parameter information of the problem
    public static parameters params = new parameters();

    // tracks the number of different combinations the solver has searched
    public static int combo = 0;
    public static int comboLimit = 0;

    public static boolean searching = true;

    public void stopSearch() {
        searching = false;
    }

    public void hideInterfaceData() {
        _interface.hideData();
    }

    // horizontal permutation: shuffles the locations horizontally on the 2d locations array
    public static void horizontalPermutations(
            Set<Tuple2<Integer, Integer>> items,
            Stack<Tuple2<Integer, Integer>> permutation,
            int size, int Hindex, int chunks,
            Tuple2<Integer, Integer>[][] currentArrangement,
            Tuple2<Integer, Integer>[][] ogArrangement) {

        if (searching) {
            if (permutation.size() == size) {
                // when permutation has been created, convert it to an array...
                Tuple2<Integer, Integer>[] permCopy =
                        permutation.toArray(new Tuple2[permutation.size()]);

                // ... then added it to the current arrangement
                for (int j = 0; j < permCopy.length; ++j)
                {
                    currentArrangement[Hindex][j] = permCopy[j];
                }

                // then move to the next row
                Hindex++;

                // Once the current arrangement has been complete...
                if (Hindex == chunks)
                {
                    // ... start with the vertical permutations

                    // setup starting data (same as for horizontal permutations, but with columns instead of rows)
                    Tuple2<Integer, Integer>[] arrayRow = new Tuple2[chunks];
                    for (int j = 0; j < chunks; ++j)
                    {
                        arrayRow[j] = currentArrangement[j][0];
                    }

                    Set<Tuple2<Integer, Integer>> firstV = new HashSet<>(Arrays.asList(arrayRow));

                    Tuple2<Integer, Integer>[][] VcurrentArrangement = new Tuple2[chunks][size];

                    int VstartingIndex = 0;

                    verticalPermutations(
                            firstV, new Stack<Tuple2<Integer, Integer>>(), firstV.size(),
                            VstartingIndex, size, VcurrentArrangement, currentArrangement);
                }
                else
                {
                    // ... otherwise move to next row

                    // get next row
                    Tuple2<Integer, Integer>[] nextArray = new Tuple2[size];
                    for (int j = 0; j < size; ++j)
                    {
                        nextArray[j] = ogArrangement[Hindex][j];
                    }

                    // convert to list (set)
                    Set<Tuple2<Integer, Integer>> nextItems =
                            new HashSet<>(Arrays.asList(nextArray));

                    // begin function
                    horizontalPermutations(nextItems, new Stack<Tuple2<Integer, Integer>>(), size,
                            Hindex, chunks, currentArrangement, ogArrangement);
                }
            }

            /* items available for permutation */
            Tuple2<Integer, Integer>[] availableItems = items.toArray(new Tuple2[0]);
            for(Tuple2<Integer, Integer> i : availableItems) {
                /* add current item */
                permutation.push(i);

                /* remove item from available item set */
                items.remove(i);

                /* pass it on for next permutation */
                horizontalPermutations(items, permutation, size,
                        Hindex, chunks, currentArrangement, ogArrangement);

                /* pop and put the removed item back */
                items.add(permutation.pop());
            }
        }
    }

    // vertical permutation: shuffles the locations vertically on the 2d locations array
    // capacity and distance checking done here after it has a complete arrangement
    public static void verticalPermutations(
            Set<Tuple2<Integer, Integer>> items,
            Stack<Tuple2<Integer, Integer>> permutation,
            int size, int Vindex, int chunkSize,
            Tuple2<Integer, Integer>[][] currentArrangement,
            Tuple2<Integer, Integer>[][] ogArrangement) {

        if (searching) {
            /* permutation stack has become equal to size that we require */
            if(permutation.size() == size) {

                // when permutation has been created, convert it to an array...
                Tuple2<Integer, Integer>[] permCopy =
                        permutation.toArray(new Tuple2[permutation.size()]);

                // ... then added it to the current arrangement
                for (int j = 0; j < permCopy.length; ++j)
                {
                    currentArrangement[j][Vindex] = permCopy[j];
                }

                // then move to the next column
                Vindex++;

                // Once the current arrangement has been complete...
                if (Vindex == chunkSize)
                {

                    // take the current combination and apply the constraint checking

                    // increment combination count
                    combo++;

                    // limits search time
                    if (comboLimit != 0 && combo >= comboLimit)
                    {
                        System.out.println("Final Arrangement!");
                        searching = false;
                    }

                    // constraints to check: capacity, distance (may use choco for each vehicle path)
                    // checks the total capacity of each arrangement of locations
                    if (withinVehicleCapacities(currentArrangement))
                    {
                        // arrange each chunk so that it forms the shortest path for each vehicle
                        findShortestArrangement(currentArrangement);
                    }
                    else
                    {
                        // one of the arrangement paths exceeds the capacity limit of the vehicles
                        //System.out.println("Arrangement isn't viable, vehicle capacity breached!");
                    }
                }
                else
                {
                    // ... otherwise move to next column
                    Tuple2<Integer, Integer>[] nextArray = new Tuple2[size];
                    for (int j = 0; j < size; ++j)
                    {
                        nextArray[j] = ogArrangement[j][Vindex];
                    }

                    // convert to list (set)
                    Set<Tuple2<Integer, Integer>> nextItems =
                            new HashSet<>(Arrays.asList(nextArray));

                    // begin function
                    verticalPermutations(nextItems, new Stack<Tuple2<Integer, Integer>>(), size,
                            Vindex, chunkSize, currentArrangement, ogArrangement);
                }
            }

            /* items available for permutation */
            Tuple2<Integer, Integer>[] availableItems = items.toArray(new Tuple2[0]);
            for(Tuple2<Integer, Integer> i : availableItems) {
                /* add current item */
                permutation.push(i);

                /* remove item from available item set */
                items.remove(i);

                /* pass it on for next permutation */
                verticalPermutations(items, permutation, size,
                        Vindex, chunkSize, currentArrangement, ogArrangement);

                /* pop and put the removed item back */
                items.add(permutation.pop());
            }
        }
    }

    // checks if each vehicles' route is within capacity limit
    public static boolean withinVehicleCapacity(Tuple2<Integer, Integer>[][] arrangement)
    {
        // goes through each path...
        for (Tuple2<Integer, Integer>[] locationSet : arrangement){
            // ... and sums up that paths total capacity
            int pathCapacity = 0;
            for (Tuple2<Integer, Integer> location : locationSet){
                pathCapacity += location.getSecondEntity();
            }

            // if it's higher than the vehicles' capacity then the combination is invalid
            if (pathCapacity > params.vehicleCapacity)
            {
                return false;
            }
        }
        return true;
    }

    public static int[] vehicleRouteArrangements;

    public static int[] convertToRouteToVehicle(int[] vehicleToRoute){

        int[] routeToVehicle = new int[vehicleToRoute.length];

        int vehicleIndex = 1;
        for (int route : vehicleToRoute)
        {
            if ((route-1) >= 0)
            {
                routeToVehicle[route-1] = vehicleIndex;
                vehicleIndex++;
            }
            else
            {
                System.out.println("Index error...");
            }
        }

        return routeToVehicle;
    }

    // checks if each vehicles' route is within capacity limit
    public static boolean withinVehicleCapacities(Tuple2<Integer, Integer>[][] arrangement)
    {
        // checks if routes are within vehicles capacity, and finds the best fit route for
        // the vehicle (the closer the vehicles' capacity to the routes' capacity the better)

        /*for (Tuple2<Integer, Integer>[] i : arrangement)
        {
            System.out.println(Arrays.toString(i));
        }*/

        // vehicles are numbered in the order of the array and routes are numbered
        // in the order they are in the arrangement array.
        // e.g. vehicle 1 -> vehicleAssignments[0] -> route 3 (index),
        //      arrangement[2] -> route 3
        int[] vehicleAssignments = new int[params.vehicles];

        // set all assignments to zero
        for (int i = 0; i < vehicleAssignments.length; ++i) {
            vehicleAssignments[i] = 0;
        }

        // sum up capacities of all routes
        int[] routeCapacities = new int[arrangement.length];
        int routeCapIndex = 0;
        for (Tuple2<Integer, Integer>[] locationSet : arrangement) {
            int pathCapacity = 0;
            for (Tuple2<Integer, Integer> location : locationSet) {
                if (location.getFirstEntity() != 0)
                {
                    //System.out.print(location);
                    pathCapacity += location.getSecondEntity();
                }
            }
            routeCapacities[routeCapIndex] = pathCapacity;
            routeCapIndex++;
            //System.out.println(pathCapacity);
        }

        int vehicleIndex = 0;
        // finds the best fit route for each vehicle
        for (int v : params.vehicleCapacities) {
            int routeIndex = 0;
            int bestRouteAssignment = 0;
            int bestFitRoute = 0;
            for (int route: routeCapacities) {

                boolean skipRoute = false;

                // doesn't check route if route is already assigned
                for (int i = 0; i < vehicleAssignments.length; ++i)
                {
                    if (vehicleAssignments[i] == (routeIndex + 1))
                    {
                        skipRoute = true;
                    }
                }

                if (skipRoute)
                {
                    routeIndex++;
                    continue;
                }

                // if route is within vehicle capacity limit
                if (v >= route)
                {
                    // if route is a better fit
                    if (route > bestFitRoute || bestFitRoute == 0)
                    {
                        bestRouteAssignment = routeIndex + 1;;
                        bestFitRoute = route;
                    }
                }
                routeIndex++;
            }

            // assign best route
            vehicleAssignments[vehicleIndex] = bestRouteAssignment;

            vehicleIndex++;
        }

        for (int va : vehicleAssignments)
        {
            if (va == 0) {
                return false;
            }
        }

        vehicleRouteArrangements = convertToRouteToVehicle(vehicleAssignments);

        return true;
    }

    // global variables for the current best solution
    public static int[][] bestArrangement;      // best location arrangement for each vehicle
    public static int bestArrangementDist = 0;  // total distance of best arrangement
    public static int[] bestArrangementOfVehicles; // best arrangement of vehicles to routes
    public static int[] bestRouteDist;
	
    // displays the current best arrangement and distance
    public static void displayBestArrangement() {
        if (bestArrangement != null) {
            _interface.newBestArrangement(bestArrangement, combo, bestArrangementDist, bestArrangementOfVehicles, bestRouteDist);

            System.out.println("Combination: " + combo);
            System.out.println("Vehicle paths: ");
            int routeIndex = 0;
            for (int[] x : bestArrangement)
            {
                System.out.println(
                        "V"+ bestArrangementOfVehicles[routeIndex] +
                                ": " + Arrays.toString(x) + " -> " + bestRouteDist[routeIndex]);
                routeIndex++;
            }
            System.out.println("Total Distance: " + bestArrangementDist);
        }
    }

    // finds the shortest path for each used vehicle
    public static void findShortestArrangement(Tuple2<Integer, Integer>[][] arrangement) {
        // stores total distance
        int totalDistance = 0;
        // stores the shortest path arrangements
        int[][] pathArrangement = new int[arrangement.length][];

        int[] routeDist = new int[arrangement.length];

        // tracks the path it's optimizing
        int pathIndex = 0;

        // goes through each path to find the most optimal routes using the choco model
        for (Tuple2<Integer, Integer>[] path : arrangement)
        {
            // create new model for path
            String modelName = "Route, C:" + combo + ", P:" + pathIndex;
            Model pathModel = new Model(modelName);

            // remove all dummy locations
            Stack<Tuple2<Integer, Integer>> nonDummyList = new Stack<>();
            for (Tuple2<Integer, Integer> loc : path)
            {
                if (loc.getFirstEntity() != 0)
                {
                    nonDummyList.push(loc);
                }
            }

            Tuple2<Integer, Integer>[] nonDummyArray =
                    nonDummyList.toArray(new Tuple2[nonDummyList.size()]);

            // get the index of each location and copy it into an array
            Tuple2<Integer, Integer>[] locats = new Tuple2[nonDummyArray.length + 1];
            locats[0] = new Tuple2<>(0, 0);
            for (int i = 0; i <  nonDummyArray.length; ++i)
            {
                locats[i+1] = new Tuple2<>(i+1, nonDummyArray[i].getFirstEntity());
            }

            // find the longest distance between all locations in the path
            int maxdist = 0;
            for (Tuple2<Integer, Integer> loc : locats)
            {
                for (Tuple2<Integer, Integer> nextloc : locats)
                {
                    if (params.distances[loc.getSecondEntity()][nextloc.getSecondEntity()] > maxdist)
                    {
                        maxdist = params.distances[loc.getSecondEntity()][nextloc.getSecondEntity()];
                    }
                }
            }

            // VARIABLES
            // For each supplier, the next one visited in the route
            IntVar[] succ = pathModel.intVarArray(
                    "succ", locats.length, 0, locats.length - 1);
            // For each supplier, the distance to the succ visited one
            IntVar[] dist = pathModel.intVarArray(
                    "dist", locats.length, 0, maxdist);
            // Total distance of the route
            IntVar totDist = pathModel.intVar(
                    "Total distance", 0, maxdist * locats.length);

            // CONSTRAINTS
            for (int i = 0; i < locats.length; ++i)
            {
                // For each supplier, the distance to the next one should be maintained
                // this is achieved, here, with a TABLE constraint
                // Such table is inputed with a Tuples object
                // that stores all possible combinations
                Tuples tuples = new Tuples(true);
                for (int j = 0; j < locats.length; ++j)
                {
                    // For a given supplier i
                    // a couple is made of a supplier j and the distance i and j
                    if (j != i){
                        tuples.add(locats[j].getFirstEntity(),
                                params.distances[locats[i].getSecondEntity()][locats[j].getSecondEntity()]);
                    }
                }
                // The Table constraint ensures that one combination holds
                // in a solution
                pathModel.table(succ[i], dist[i], tuples).post();
            }

            // The route forms a single circuit of size (path length)
            pathModel.subCircuit(succ, 0, pathModel.intVar(locats.length)).post();
            // Defining the total distance
            pathModel.sum(dist, "=", totDist).post();

            // set objective to find the shortest distance
            pathModel.setObjective(Model.MINIMIZE, totDist);
            Solver solver =  pathModel.getSolver();
            solver.setSearch(
                    Search.intVarSearch(
                            new MaxRegret(),
                            new IntDomainMin(),
                            dist)
            );

            // creates a list of the best path for the current vehicle
            int[] bestPath = new int[locats.length + 1];
            int bestDist = 0;

            // holds the current path it is checking
            int[] curPath = new int[locats.length + 1];
            int curDist = 0;

            //solver.showShortStatistics();
            while(solver.solve()){
                int current = 0;
                // added start location (depot)
                curPath[0] = current;
                for (int j = 0; j < locats.length; j++) {
                    current = succ[current].getValue();
                    // added next location
                    curPath[j+1] = current;
                }
                // get distance
                curDist = totDist.getValue();

                // if the current path is shorter than the best, update best
                if (curDist < bestDist || bestDist == 0)
                {
                    int locIndex = 0;
                    for (int location : curPath)
                    {
                        for (int i = 0; i < locats.length; ++i)
                        {
                            if (locats[i].getFirstEntity() == location)
                            {
                                bestPath[locIndex] = locats[i].getSecondEntity();
                                locIndex++;
                                break;
                            }
                        }
                    }
                    bestDist = curDist;
                }
                // reset current variables
                curDist = 0;
                curPath = new int[locats.length + 1];
            }
            // reset Choco model
            pathModel.getSolver().reset();
            pathModel.clearObjective();
            pathModel = null;
            solver.reset();

            // store the path and added the distance
            pathArrangement[pathIndex] = bestPath;
            routeDist[pathIndex] = bestDist;
            totalDistance += bestDist;

            // move on to the next path
            pathIndex += 1;
        }

        // if the total distance of the current arrangement is shorter than
        // the best arrangement, update the best and display it.
        if (totalDistance < bestArrangementDist || bestArrangementDist == 0)
        {
            bestArrangementDist = totalDistance;
            bestRouteDist = routeDist;
            bestArrangement = pathArrangement;
            bestArrangementOfVehicles = vehicleRouteArrangements;
            displayBestArrangement();
        }
    }

    // main solver function that breaks up the locations
    // into paths and begins combination checking
    public static void MRA_Function(
            Tuple2<Integer, Integer>[] arrayToSplit, int vehicles){
        // ends function if there are no vehicles
        if (vehicles <= 0){
            System.out.println("No vehicles....");
            return;
        }
        // function first needs to break up the locations into even groups (chunks) per vehicle.
        // each chunk represents the locations a vehicle needs to visit

        // get average chunk size
        float DecChunkSize = (float)arrayToSplit.length / (float)vehicles;

        float remainingLocations = Math.round((DecChunkSize - (float)Math.floor(DecChunkSize)) * (float)vehicles);

        // get average chunk size
        float maxChunkSize = (float)Math.ceil((float)arrayToSplit.length / (float)vehicles);

        float minChunkSize = (float)Math.floor((float)arrayToSplit.length / (float)vehicles);

        //System.out.println(chunkSize);

        // determine the number of vehicles that will be used
        int chunks = 0;

        if (vehicles > arrayToSplit.length)
        {
            chunks = arrayToSplit.length;
        }
        else
        {
            chunks = vehicles;
        }


        // break up the indexed supplier capacity array into chunks and store it in a 2d array
        Tuple2<Integer, Integer>[][] arrays = new Tuple2[chunks][];

        // Breaks up the locations into even chunks, if one chunk is smaller than the rest it adds a dummy location

        // dummy locations are used to make the 2d array of locations an even rectangle so the
        // permutation functions can process it

        boolean excessLoc = false;
        if (remainingLocations != 0) {
            excessLoc = true;
        }

        // if locations can't be broken up into even chunks between vehicles...
        if (excessLoc) {
            // ...account for uneven chunks with dummy locations

            for(int i = 0; i < remainingLocations; i++) {
                // this copies 'chunk' times 'chunkSize' elements into a new array
                arrays[i] = Arrays.copyOfRange(
                        arrayToSplit, (int)(i * maxChunkSize), (int)(i * maxChunkSize + maxChunkSize));
            }

            Tuple2<Integer, Integer>[] carrier;

            int dummyIndex = 0;

            Tuple2<Integer, Integer> dummyEle = new Tuple2(0, dummyIndex);

            int stepReduction = 0;
            for(int j = (int)remainingLocations; j < chunks; j++) {
                // this copies 'chunk' times 'chunkSize' elements into a new array
                carrier = Arrays.copyOfRange(
                        arrayToSplit,
                        (int)(j * maxChunkSize - stepReduction),
                        (int)(j * maxChunkSize + minChunkSize - stepReduction));

                Stack<Tuple2<Integer, Integer>> carried = new Stack<>();

                for(Tuple2<Integer, Integer> i : carrier)
                {
                    carried.push(i);
                }
                carried.push(dummyEle);

                arrays[j] = carried.toArray(new Tuple2[carried.size()]);

                dummyIndex++;
                dummyEle = new Tuple2(0, dummyIndex);

                stepReduction++;
            }
        }
        else {
            // ...break it up evenly
            for(int i = 0; i < chunks; i++) {
                // this copies 'chunk' times 'chunkSize' elements into a new array
                arrays[i] = Arrays.copyOfRange(
                        arrayToSplit, (int)(i * maxChunkSize), (int)(i * maxChunkSize + maxChunkSize));
            }
        }

        // next, the function search through the different combinations of locations between chunks.
        // this is done with the use of two permutation functions:
        //      - horizontalPermutations: shuffles the locations horizontally
        //      - verticalPermutations: shuffles the locations vertically
        // the use of these two functions allows the solver to search through all possible combinations
        // of locations between different chunks.

        // the locations in chunks are shuffled horizontally (each row from right to left)
        // then shuffled vertically (each column from top to bottom), when there is a complete
        // combination/arrangement, it's checked against the hard and soft constrains (capacity and shortest distance)

        // to start the permutation sequence, the starting data must be formed and fed into the function

        // needs tweaking

        // Starting data:
        // get the first row of from the 2d indexed capacity array
        Tuple2<Integer, Integer>[] arrayRow = new Tuple2[(int)maxChunkSize];
        for (int j = 0; j < (int)maxChunkSize; ++j)
        {
            arrayRow[j] = arrays[0][j];
        }
        // transfer the data into a list (set)
        Set<Tuple2<Integer, Integer>> firstH = new HashSet<>(Arrays.asList(arrayRow));

        // create a new empty 2d array that will hold the combination data
        Tuple2<Integer, Integer>[][] HcurrentArrangement = new Tuple2[chunks][(int)maxChunkSize];

        // index used to keep track of which row the permutation is modifying
        int HstartingIndex = 0;

        // start permutation sequence (begin with the horizontal permutations)
        horizontalPermutations(
                firstH, new Stack<Tuple2<Integer, Integer>>(), firstH.size(),
                HstartingIndex, chunks, HcurrentArrangement, arrays);
    }

    // prints the indexed supplier capacity array
    public static void printSupplierCapacity(Tuple2<Integer, Integer>[] supCap) {
        for (Tuple2<Integer, Integer> location : supCap)
        {
            System.out.print(
                    "Location: " + location.getFirstEntity() +
                            ", Capacity: " + location.getSecondEntity() + "\n");
        }
    }

    public VRP_Test3(
            int numLocations, int numVehicles,
            int[][] coords, int distance,
            int[] lCapacity, int[] vCapacity) {

        params.suppliers = 0;
        params.vehicles = 0;
        params.coordinates = null;
        params.vehicleCapacities = null;
        params.supplierCapacity = null;
        params.indSupCap = null;

        params.suppliers = numLocations;
        params.vehicles = numVehicles;
        params.vehicleCapacities = vCapacity;
        params.supplierCapacity = lCapacity;
        params.formLocationCapacity();
        params.coordinates = coords;

        // Generate a distance matrix
        params.distances = new int[numLocations][numLocations];
        for (int i = 0; i < numLocations; i++) {
            for (int j = 0; j < numLocations; j++) {
                params.distances[i][j] = (int)Math.hypot(
                        coords[i][0] - coords[j][0], coords[i][1] - coords[j][1]);
            }
        }

        _interface = new SolverInterface(numVehicles, numLocations, coords, distance);
        
        solve();
    }

    public static void solve() 
    {
        /*
        Cluster system breakdown:
            1.	Break up each location into groups, 1 per vehicle
            2.	Search through all the possible combinations of locations between each of those groups
            3.	Exclude the combinations that exceed the capacity limit of the vehicles
            4.  For each group, find the best arrangement of the locations to find the shortest path for the vehicle
            5.	If a better (more optimal) combination is found, then update the best solution and display it
        */
    	
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        ContainerController containerController = runtime.createMainContainer(profile);

        AgentController mraController;
        try 
        {
            mraController = containerController.createNewAgent("MRA", "VRP.messaging.MasterRoutingAgent", null);
            mraController.start();
        }
        catch (StaleProxyException e) 
        {
            e.printStackTrace();
        }
        
        for(int i = 0; i != params.vehicles; i++)
        {
            AgentController daController;
            try 
            {
                daController = containerController.createNewAgent("DA#" + i, "VRP.messaging.DeliveryAgent", null);
                daController.start();    
            } 
            catch (StaleProxyException e) 
            {
                e.printStackTrace();
            }
         }
    }
}
