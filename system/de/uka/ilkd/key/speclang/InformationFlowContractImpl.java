// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang;

import java.util.HashMap;
import java.util.Map;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.pp.LogicPrinter;
import de.uka.ilkd.key.proof.OpReplacer;

/**
 * Standard implementation of the DependencyContract interface.
 */
public final class InformationFlowContractImpl implements InformationFlowContract {
    
    private static final TermBuilder TB = TermBuilder.DF;
    
    private final String baseName;    
    private final String name;
    private final KeYJavaType kjt;
    private final ObserverFunction target;
    private final Term originalPre;
    private final Term originalMby;    
    private final Term originalDep;
    private final ProgramVariable originalSelfVar;
    private final ImmutableList<ProgramVariable> originalParamVars;
    private final int id;    
    

    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------
    
    private InformationFlowContractImpl(String baseName,
	                           String name, 
	                           KeYJavaType kjt,
	    			   ObserverFunction target,
	    			   Term pre,
	                  	   Term mby,	    			   
	                  	   Term dep,
	                  	   ProgramVariable selfVar,
	                  	   ImmutableList<ProgramVariable> paramVars,
	                  	   int id) {
	assert baseName != null;
	assert kjt != null;
	assert target != null;
	assert pre != null;
	assert dep != null;
        assert (selfVar == null) == target.isStatic();
        assert paramVars != null;
        assert paramVars.size() == target.arity() - (target.isStatic() ? 1 : 2);
	this.baseName = baseName;
        this.name                   = name != null 
                                      ? name 
                                      : baseName + " [id: " + id + " / " 
                                        + target
                                        + " for " 
                                        + kjt.getJavaType().getName() 
                                        + "]";
	this.kjt = kjt;
	this.target = target;
	this.originalPre = pre;
	this.originalMby = mby;	
	this.originalDep = dep;
	this.originalSelfVar = selfVar;
	this.originalParamVars = paramVars;
	this.id = id;
    }
    
    
    public InformationFlowContractImpl(String baseName, 
	                          KeYJavaType kjt,
	    			  ObserverFunction target,
	    			  Term pre,
	                  	  Term mby,	    			  
	                  	  Term dep,
	                  	  ProgramVariable selfVar,
	                  	  ImmutableList<ProgramVariable> paramVars) {
	this(baseName, 
             null, 
             kjt, 
             target, 
             pre, 
             mby,             
             dep, 
             selfVar, 
             paramVars, 
             INVALID_ID);
    }    
    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------    
    
    @Override
    public String getName() {
	return name;
    }
    
    
    @Override
    public int id() {
	return id;
    }
    

    @Override
    public KeYJavaType getKJT() {
	return kjt;
    }

    
    @Override
    public ObserverFunction getTarget() {
	return target;
    }
    
    
    @Override
    public boolean hasMby() {
	return originalMby != null;
    }
    
    
    @Override
    public Term getPre(ProgramVariable selfVar, 
	    	       ImmutableList<ProgramVariable> paramVars,
	    	       Services services) {
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
	Map map = new HashMap();
	map.put(originalSelfVar, selfVar);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(originalParamVar, paramVars.head());
	    paramVars = paramVars.tail();
	}
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalPre);
    }
    
    
    @Override
    public Term getPre(Term heapTerm,
	               Term selfTerm, 
	    	       ImmutableList<Term> paramTerms,
	    	       Services services) {
	assert heapTerm != null;
	assert (selfTerm == null) == (originalSelfVar == null);
	assert paramTerms != null;
	assert paramTerms.size() == originalParamVars.size();
	assert services != null;
	Map map = new HashMap();
	map.put(TB.heap(services), heapTerm);
	map.put(TB.var(originalSelfVar), selfTerm);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(TB.var(originalParamVar), paramTerms.head());
	    paramTerms = paramTerms.tail();
	}	
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalPre);
    }
    
    

    @Override
    public Term getMby(ProgramVariable selfVar,
	               ImmutableList<ProgramVariable> paramVars,
	               Services services) {
	assert hasMby();
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
	Map map = new HashMap();
	map.put(originalSelfVar, selfVar);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(originalParamVar, paramVars.head());
	    paramVars = paramVars.tail();
	}
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalMby);
    }
    

    @Override
    public Term getMby(Term heapTerm,
	               Term selfTerm, 
	               ImmutableList<Term> paramTerms, 
	               Services services) {
	assert hasMby();
	assert heapTerm != null;
	assert (selfTerm == null) == (originalSelfVar == null);
	assert paramTerms != null;
	assert paramTerms.size() == originalParamVars.size();
	assert services != null;
	Map map = new HashMap();
	map.put(TB.heap(services), heapTerm);
	map.put(TB.var(originalSelfVar), selfTerm);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(TB.var(originalParamVar), paramTerms.head());
	    paramTerms = paramTerms.tail();
	}	
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalMby);
    }    
    
    
    @Override
    public InformationFlowContract setID(int newId) {
        return new InformationFlowContractImpl(baseName,
        	                          null,
                			  kjt,        	                         
                			  target,
                			  originalPre,
                			  originalMby,                			  
                			  originalDep,
                			  originalSelfVar,
                			  originalParamVars,
                			  newId);	
    }
    
    
    @Override
    public InformationFlowContract setTarget(KeYJavaType newKJT,
	    		      	        ObserverFunction newTarget, 
	    		      	        Services services) {
        return new InformationFlowContractImpl(baseName,
        				  null,
                			  newKJT,        				 
                			  newTarget,
                			  originalPre,
                			  originalMby,                			  
                			  originalDep,
                			  originalSelfVar,
                			  originalParamVars,
                			  id);	
    }        
    
    
    @Override
    public String getHTMLText(Services services) {
	final String pre = LogicPrinter.quickPrintTerm(originalPre, services)
				       .replace("\n", " ");
        final String mby = hasMby() 
        	           ? LogicPrinter.quickPrintTerm(originalMby, services)
        	        	         .replace("\n", " ")
        	           : null;
        final String dep = LogicPrinter.quickPrintTerm(originalDep, services)
         			       .replace("\n", " ");
                      
        return "<html>"
                + "<b>pre</b> "
                + LogicPrinter.escapeHTML(pre)
                + "<br><b>dep</b> "
                + LogicPrinter.escapeHTML(dep)
                + (hasMby() 
                   ? "<br><b>measured-by</b> " + LogicPrinter.escapeHTML(mby)
                   : "")                
                + "</html>";
    }    
    
    
    @Override
    public boolean toBeSaved() {
	return false; //because dependency contracts currently cannot be
	              //specified directly in DL 
    }
    
    
    @Override
    public String proofToString(Services services) {
	assert false;
	return null;
    }
    

    @Override
    public Term getDep(ProgramVariable selfVar,
	               ImmutableList<ProgramVariable> paramVars,
	               Services services) {
        assert (selfVar == null) == (originalSelfVar == null);
        assert paramVars != null;
        assert paramVars.size() == originalParamVars.size();
        assert services != null;
	Map map = new HashMap();
	map.put(originalSelfVar, selfVar);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(originalParamVar, paramVars.head());
	    paramVars = paramVars.tail();
	}
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalDep);
    }
    

    @Override
    public Term getDep(Term heapTerm,
	               Term selfTerm, 
	               ImmutableList<Term> paramTerms, 
	               Services services) {
	assert heapTerm != null;
	assert (selfTerm == null) == (originalSelfVar == null);
	assert paramTerms != null;
	assert paramTerms.size() == originalParamVars.size();
	assert services != null;
	Map map = new HashMap();
	map.put(TB.heap(services), heapTerm);
	map.put(TB.var(originalSelfVar), selfTerm);
	for(ProgramVariable originalParamVar : originalParamVars) {
	    map.put(TB.var(originalParamVar), paramTerms.head());
	    paramTerms = paramTerms.tail();
	}	
	OpReplacer or = new OpReplacer(map);
	return or.replace(originalDep);
    }    
    
    
    @Override
    public String toString() {
	return originalDep.toString();
    }


    @Override
    public Term getDeclassification(ProgramVariable selfVar,
            ImmutableList<ProgramVariable> paramVars, Services services) {
	// TODO Auto-generated method stub
	return null;
    }


    @Override
    public Term getDeclassification(Term heapTerm, Term selfTerm,
            ImmutableList<Term> paramTerms, Services services) {
	// TODO Auto-generated method stub
	return null;
    }


    @Override
    public Term getMod(ProgramVariable selfVar,
            ImmutableList<ProgramVariable> paramVars, Services services) {
	// TODO Auto-generated method stub
	return null;
    }


    @Override
    public Term getMod(Term heapTerm, Term selfTerm,
            ImmutableList<Term> paramTerms, Services services) {
	// TODO Auto-generated method stub
	return null;
    }


    @Override
    public Term getParameterDeps(ProgramVariable selfVar,
            ImmutableList<ProgramVariable> paramVars, Services services) {
	// TODO Auto-generated method stub
	return null;
    }


    @Override
    public Term getParameterDeps(Term heapTerm, Term selfTerm,
            ImmutableList<Term> paramTerms, Services services) {
	// TODO Auto-generated method stub
	return null;
    }
}