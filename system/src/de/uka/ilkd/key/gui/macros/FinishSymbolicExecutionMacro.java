package de.uka.ilkd.key.gui.macros;

import de.uka.ilkd.key.gui.KeYMediator;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.PosInOccurrence;
import de.uka.ilkd.key.logic.Sequent;
import de.uka.ilkd.key.logic.SequentFormula;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.op.Modality;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.rule.RuleApp;
import de.uka.ilkd.key.strategy.RuleAppCost;
import de.uka.ilkd.key.strategy.RuleAppCostCollector;
import de.uka.ilkd.key.strategy.Strategy;
import de.uka.ilkd.key.strategy.TopRuleAppCost;

/**
 * The macro FinishSymbolicExecutionMacro continues automatic rule application
 * until there is no more modality on the sequent.
 * 
 * This is done by implementing a delegationg {@link Strategy} which assigns to
 * any rule application infinite costs if there is no modality on the sequent.
 * 
 * @author mattias ulbrich
 */
public class FinishSymbolicExecutionMacro extends StrategyProofMacro {

    @Override 
    public String getName() {
        return "Finish symbolic execution";
    }

    @Override 
    public String getDescription() {
        return "Continue automatic strategy application until no more modality is on the sequent.";
    }

    /*
     * find a modality term in a node
     */
    private static boolean hasModality(Node node) {
        Sequent sequent = node.sequent();
        for (SequentFormula sequentFormula : sequent) {
            if(hasModality(sequentFormula.formula())) {
                return true;
            }
        }

        return false;
    }

    /*
     * recursively descent into the term to detect a modality.
     */
    private static boolean hasModality(Term term) {
        if(term.op() instanceof Modality) {
            return true;
        }

        for (Term sub : term.subs()) {
            if(hasModality(sub)) {
                return true;
            }
        }

        return false;
    }

    @Override 
    protected Strategy createStrategy(KeYMediator mediator, PosInOccurrence posInOcc) {
        return new FilterSymbexStrategy(
                mediator.getInteractiveProver().getProof().getActiveStrategy());
    }

    /**
     * The Class FilterAppManager is a special strategy assigning to any rule
     * infinite costs if the goal has no modality
     */
    private static class FilterSymbexStrategy implements Strategy {

        private static final Name NAME = new Name(FilterSymbexStrategy.class.getSimpleName());

        private final Strategy delegate;

        public FilterSymbexStrategy(Strategy delegate) {
            this.delegate = delegate;
        }


        @Override 
        public Name name() {
            return NAME;
        }

        @Override 
        public RuleAppCost computeCost(RuleApp app, PosInOccurrence pio, Goal goal) {
            if(!hasModality(goal.node())) {
                return TopRuleAppCost.INSTANCE;
            }

            return delegate.computeCost(app, pio, goal);
        }

        // just to make sure double check ...
        @Override 
        public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
            if(!hasModality(goal.node())) {
                return false;
            }
            
            return delegate.isApprovedApp(app, pio, goal);
        }

        @Override 
        public void instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
                RuleAppCostCollector collector) {
            delegate.instantiateApp(app, pio, goal, collector);
        }
    }

}