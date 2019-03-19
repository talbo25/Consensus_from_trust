import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    HashMap<Integer, Set<Transaction>> node_tracker;
    Set<Transaction> pendingTrx;
    boolean[] followees;
    double p_graph;
    double p_malicious;
    double p_txDistribution;
    double numRounds;
    double p_sample;
    int check_samples_limit;
    int[] strikes;
    int current_round;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
        this.node_tracker = new HashMap<>();
        this.current_round = 0;

        check_samples_limit = (int) java.lang.Math.round((0.5 + (p_graph / 2)) * numRounds);
        p_sample = (numRounds * p_malicious) / check_samples_limit;


    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
        this.strikes = new int[this.followees.length];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTrx = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        Set<Transaction> transactions_to_followers = new HashSet<>(this.pendingTrx);
        ++current_round;
        if (current_round < numRounds) {
            this.pendingTrx.clear();
        }
        return transactions_to_followers;

    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        HashMap<Integer, Set<Transaction>> check_candidates = new HashMap<>();
        boolean[] check_followees = new boolean[this.followees.length];

        boolean Samples_check = false;

        if (current_round < check_samples_limit && current_round > 1 && (Math.random() < p_sample || current_round == 2)) {
            Samples_check = true;
        }

        for (Candidate candidate : candidates) {

            // if already marked as a malicious
            if (!followees[candidate.sender]) {
                continue;
            }
            check_followees[candidate.sender] = true;
            // check if move transactions as is, or compare a sample or clean pending transactions
            if (Samples_check || current_round == 2) {
                if (!check_candidates.containsKey(candidate.sender)) {
                    check_candidates.put(candidate.sender, new HashSet<>());
                }
                check_candidates.get(candidate.sender).add(candidate.tx);
            }
            // normal iteration
            else {
                //initialize node_tracker with first iteration values
                if (current_round == 1) {
                    if (!node_tracker.containsKey(candidate.sender)) {
                        node_tracker.put(candidate.sender, new HashSet<>());
                    }
                    node_tracker.get(candidate.sender).add(candidate.tx);
                }
                pendingTrx.add(candidate.tx); //maybe we send the same trx multiple times
            }
        }

        // if send a null transaction
        for (int i = 0; i < this.followees.length; ++i) {
            if (followees[i] && !check_followees[i]) {
                followees[i] = false;
            }
        }

        // Samples check
        if (Samples_check) {
            checkMalicious_Samples(check_candidates);
            for (int sender : check_candidates.keySet()) {
                if (followees[sender]) {
                    pendingTrx.addAll(check_candidates.get(sender));
                }
            }
        }
    }

    public void checkMalicious_Samples(HashMap<Integer, Set<Transaction>> candidates) {
        int counter = 0;
        // check if the number of transactions from followee is equal to the first round
        // if true - check if all the transactions are equal
        // if second condition also true - add to the strikes count
        // after reach 2 strikes (== at least in two iterations the transactions are completely the same as at the first iteration)
        // == malicious node
        for (int sender : candidates.keySet()) {
            if (candidates.get(sender).size() == node_tracker.get(sender).size()) {
                for (Transaction transaction : candidates.get(sender)) {
                    if (node_tracker.get(sender).contains(transaction)) {
                        counter++;
                    }
                }
                if (counter == candidates.get(sender).size()) {
                    strikes[sender]++;

                    if (strikes[sender] >= 2) {
                        followees[sender] = false;
                    }
                }
            }
        }
    }
}

