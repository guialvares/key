package de.uka.ilkd.key.symbolic_execution.model;

import de.uka.ilkd.key.symbolic_execution.SymbolicExecutionTreeBuilder;
import de.uka.ilkd.key.symbolic_execution.model.impl.ExecutionBranchCondition;

/**
 * <p>
 * A node in the symbolic execution tree which represents a branch condition,
 * e.g. {@code x < 0}.
 * </p>
 * <p>
 * The default implementation is {@link ExecutionBranchCondition} which
 * is instantiated via a {@link SymbolicExecutionTreeBuilder} instance.
 * </p>
 * @author Martin Hentschel
 * @see SymbolicExecutionTreeBuilder
 * @see ExecutionBranchCondition
 */
public interface IExecutionBranchCondition extends IExecutionNode {
}