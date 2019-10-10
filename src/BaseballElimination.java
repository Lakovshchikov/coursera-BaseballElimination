import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.ST;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;

public class BaseballElimination {

    private class Team {
        private String name;
        private int lose;
        private int won;
        private int lost;
        private int[] games;
        private int index;
        private int vertex;

        private Team(String name, int lose, int won, int lost, int[] games, int index) {
            this.name = name;
            this.lose = lose;
            this.won = won;
            this.lost = lost;
            this.games = games;
            this.index = index;
        }

        private Team(){

        }

        public void setVertex(int vertex) {
            this.vertex = vertex;
        }
    }

    private ST<String, Team> teams;
    private int t;
    private FordFulkerson FF;
    private Queue<String> ifTrivial;

    public BaseballElimination(String filename) {
        In in = new In(filename);
        int count = in.readInt();
        teams = new ST<String, Team>();
        int index = 0;
        for (int j = 0; j < count; j++){
            String name = in.readString();
            int won = in.readInt();
            int lose = in.readInt();
            int lost = in.readInt();
            int[] games = new int[count];
            for (int i = 0; i < count; i++) {
                games[i] = in.readInt();
            }
            Team team = new Team(name, lose, won, lost, games, index);
            teams.put(name, team);
            index++;
        }
    }

    public int numberOfTeams() {
        return this.teams.size();
    }

    public Iterable<String> teams() {
        return teams.keys();
    }

    public int wins(String team) {
        checkInput(team);
        return teams.get(team).won;
    }

    public int losses(String team){
        checkInput(team);
        return teams.get(team).lose;
    }

    public int remaining(String team) {
        checkInput(team);
        return teams.get(team).lost;
    }

    public int against(String team1, String team2) {
        checkInput(team1, team2);
        Team t1 = teams.get(team1);
        Team t2 = teams.get(team2);
        return t1.games[t2.index];
    }

    public boolean isEliminated(String team) {
        if (testTrivial(team)) {
            return true;
        }
        checkInput(team);
        FlowNetwork FN = createNetwork(team);
        FF = new FordFulkerson(FN, 0, t);

        Iterable<FlowEdge> edges = FN.adj(0);
        for (FlowEdge e : edges) {
            if (e.capacity() != e.flow()){
                return true;
            }
        }
        return false;
    }

    public Iterable<String> certificateOfElimination(String team) {
        checkInput(team);
        ifTrivial = new Queue<>();
        if (isEliminated(team)) {
            if (ifTrivial.size() != 0) {
                return ifTrivial;
            }
            Queue<String> res = new Queue<String>();
            Queue<Integer> q = new Queue<Integer>();
            for (int i = 0; i < t; i++) {
                if (FF.inCut(i)) {
                    q.enqueue(i);
                }
            }
            for (Integer i : q) {
                if (i!=0) {
                    String name = findNameByVertex(i);
                    if ( name != null) {
                        res.enqueue(name);
                    }
                }
            }
            return res;
        }
        return null;
    }

    private FlowNetwork createNetwork (String team) {
        Queue<FlowEdge> edges = new Queue<FlowEdge>();
        ST<Integer, int[]> games = new ST<Integer, int[]>();
        int index = 1;
        Team testTeam = teams.get(team);
        int indexTeam = testTeam.index;
        for (String t : teams) {
            Team team1 = teams.get(t);
            for (int i = 0; i < teams.size(); i++){
                if (team1.index != indexTeam && i > team1.index && i != indexTeam) {
                    edges.enqueue(new FlowEdge(0,index,team1.games[i]));
                    int[] game = new int[2];
                    game[0] = team1.index;
                    game[1] = i;
                    games.put(index, game);
                    index++;
                }
            }
        }

        int[] points = new int[ teams.size()];

        for (int g : games) {
            int [] tms = games.get(g);
            if (points[tms[0]] == 0) {
                findTeamByIndex(tms[0]).setVertex(index);
                points[tms[0]] = index;
                index++;
            } if (points[tms[1]] == 0) {
                findTeamByIndex(tms[1]).setVertex(index);
                points[tms[1]] = index;
                index++;
            }
        }
        for (int g : games) {
            int [] tms = games.get(g);
            edges.enqueue(new FlowEdge(g,points[tms[0]], Double.POSITIVE_INFINITY));
            edges.enqueue(new FlowEdge(g,points[tms[1]], Double.POSITIVE_INFINITY));
        }

        for (int i = 0; i < points.length; i++) {
            if (points[i] != 0) {
                Team curTeam = findTeamByIndex(i);
                int capacity = testTeam.won + testTeam.lost - curTeam.won;
                if (capacity <= 0) {
                    capacity = 0;
                }
                edges.enqueue(new FlowEdge(points[i],index, capacity));
            }
        }

        FlowNetwork FN = new FlowNetwork(index + 1);
        for (FlowEdge e : edges) {
            FN.addEdge(e);
        }
        t = index;
        return FN;
    }

    private Team findTeamByIndex(int index) {
        for (String t : teams) {
            if (teams.get(t).index == index)
                return teams.get(t);
        }
        return null;
    }

    private String findNameByVertex(int vertex) {
        for (String t : teams) {
            if (teams.get(t).vertex == vertex) {
                return teams.get(t).name;
            }
        }
        return null;
    }

    private boolean testTrivial (String team) {
        int current = 0;
        ifTrivial = new Queue<>();
        Team t = teams.get(team);
        Team teamMax = new Team();
        int maxWins = t.won + t.lost;

        for (String name : teams) {
            current = teams.get(name).won;
            if (maxWins < current){
                ifTrivial.enqueue(teams.get(name).name);
            }
        }

        if (ifTrivial.size() != 0) {
            return true;
        } else {
            return false;
        }
    }

    private void checkInput (String team) {
        if (team == null || teams.get(team) == null)
            throw new IllegalArgumentException();
    }

    private void checkInput (String team1, String team2) {
        if (team1 == null || teams.get(team1) == null || team2 == null || teams.get(team2) == null)
            throw new IllegalArgumentException();
    }

    public static void main(String[] args) {
        String filename = args[0];
        BaseballElimination BE = new BaseballElimination(filename);
        String st2 = "Detroit";
        Iterable a = BE.certificateOfElimination(st2);
        int c = 0;
    }
}
