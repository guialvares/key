// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//
package de.uka.ilkd.key.speclang.jml.translation;

import java.util.Iterator;
import java.util.Map;

import antlr.Token;
import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.ArrayType;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.abstraction.PrimitiveType;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.logic.Named;
import de.uka.ilkd.key.logic.Namespace;
import de.uka.ilkd.key.logic.NamespaceSet;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.ldt.LocSetLDT;
import de.uka.ilkd.key.logic.Term;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.TermCreationException;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.Sort;
import de.uka.ilkd.key.parser.ParserException;
import de.uka.ilkd.key.proof.OpReplacer;
import de.uka.ilkd.key.speclang.PositionedString;
import de.uka.ilkd.key.speclang.translation.SLExpression;
import de.uka.ilkd.key.speclang.translation.SLTranslationException;
import de.uka.ilkd.key.speclang.translation.SLTranslationExceptionManager;
import de.uka.ilkd.key.util.*;
import de.uka.ilkd.key.util.LinkedHashMap;
import de.uka.ilkd.key.util.Pair;
import de.uka.ilkd.key.util.Triple;

import java.util.Arrays;




/**
 * Translates JML expressions to FOL.
 */
final class JMLTranslator {

    private final static JMLTranslator instance = new JMLTranslator();
    private final static TermBuilder TB = TermBuilder.DF;
    private SLTranslationExceptionManager excManager;

    private LinkedHashMap<String, JMLTranslationMethod> translationMethods;


    private JMLTranslator() {
        translationMethods = new LinkedHashMap<String, JMLTranslationMethod>();

        // clauses
        translationMethods.put("accessible", new JMLTranslationMethod() {
            @Override
            public Term translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Term.class, Services.class);
                Term ensuresTerm = (Term) params[0];
                Services services = (Services) params[1];
                return TB.convertToFormula(ensuresTerm, services);
            }
        });
        translationMethods.put("assignable", new JMLTranslationMethod() {
            @Override
            public Term translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Term.class, Services.class);
                Term ensuresTerm = (Term) params[0];
                Services services = (Services) params[1];
                return TB.convertToFormula(ensuresTerm, services);
            }
        });
        translationMethods.put("depends", new JMLTranslationMethod() {
            @Override
            public Triple<ObserverFunction, Term, Term> translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, SLExpression.class, Term.class,
                                SLExpression.class,
                                SLTranslationExceptionManager.class,
                                Services.class);
                SLExpression lhs = (SLExpression) params[0];
                Term rhs = (Term) params[1];
                SLExpression mby = (SLExpression) params[2];
                SLTranslationExceptionManager excManager =
                        (SLTranslationExceptionManager) params[3];
                Services services = (Services) params[4];

                LocationVariable heap =
                        services.getTypeConverter().getHeapLDT().getHeap();
                if (!lhs.isTerm()
                    || !(lhs.getTerm().op() instanceof ObserverFunction)
                    || lhs.getTerm().sub(0).op() != heap) {
                    throw excManager.createException("Depends clause with unexpected lhs: " + lhs);
                }
                return new Triple<ObserverFunction, Term, Term>(
                        (ObserverFunction) lhs.getTerm().op(),
                        rhs,
                        mby == null ? null : mby.getTerm());
            }
        });
        translationMethods.put("ensures", new JMLTranslationMethod() {
            @Override
            public Term translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Term.class, Services.class);
                Term ensuresTerm = (Term) params[0];
                Services services = (Services) params[1];
                return TB.convertToFormula(ensuresTerm, services);
            }
        });
        translationMethods.put("represents", new JMLTranslationMethod() {
            @Override
            public Pair translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, SLExpression.class, Term.class,
                        Services.class);
                SLExpression lhs = (SLExpression) params[0];
                Term t = (Term) params[1];

                return new Pair<ObserverFunction,Term>(
                     (ObserverFunction) lhs.getTerm().op(),
                     t);
            }
        });
        translationMethods.put("signals", new JMLTranslationMethod() {
            @Override
            public Term translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Term.class, LogicVariable.class,
                        ProgramVariable.class, KeYJavaType.class,
                        Services.class);
                Term result = (Term) params[0];
                LogicVariable eVar = (LogicVariable) params[1];
                ProgramVariable excVar = (ProgramVariable) params[2];
                KeYJavaType excType = (KeYJavaType) params[3];
                Services services = (Services) params[4];

                if (result == null) {
                    result = TB.tt();
                } else {
                    Map /* Operator -> Operator */ replaceMap =
                            new LinkedHashMap();
                    replaceMap.put(eVar, excVar);
                    OpReplacer excVarReplacer = new OpReplacer(replaceMap);

                    Sort os = excType.getSort();
                    Function instance = os.getInstanceofSymbol(services);

                    result = TB.imp(
                            TB.equals(TB.func(instance, TB.var(excVar)), TB.TRUE(
                            services)),
                            TB.convertToFormula(excVarReplacer.replace(result),
                                                services));
                }
                return result;
            }
        });
        translationMethods.put("signals_only", new JMLTranslationMethod() {
            @Override
            public Term translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                ImmutableList.class, ProgramVariable.class,
                                Services.class);
                ImmutableList<KeYJavaType> signalsonly =
                        (ImmutableList<KeYJavaType>) params[0];
                ProgramVariable excVar = (ProgramVariable) params[1];
                Services services = (Services) params[2];
                // Build appropriate term out of the parsed list of types
                // i.e. disjunction of "excVar instanceof ExcType"
                // for every ExcType in the list
                Term result = TB.ff();

                Iterator<KeYJavaType> it = signalsonly.iterator();
                while (it.hasNext()) {
                    KeYJavaType kjt = it.next();
                    Function instance = kjt.getSort().getInstanceofSymbol(
                            services);
                    result = TB.or(result,
                                   TB.equals(
                            TB.func(instance, TB.var(excVar)),
                            TB.TRUE(services)));
                }

                return result;
            }
        });

        // quantifiers
        translationMethods.put("\\forall", new JMLQuantifierTranslationMethod() {
            @Override
            public Term translateQuantifier(QuantifiableVariable qv,
                                            Term t)
                    throws SLTranslationException {
                return TB.all(qv, t);
            }
            @Override
            public Term combineQuantifiedTerms(Term t1,
                                               Term t2)
                    throws SLTranslationException {
                return TB.imp(t1, t2);
            }
        });
        translationMethods.put("\\exists", new JMLQuantifierTranslationMethod() {
            @Override
            public Term translateQuantifier(QuantifiableVariable qv,
                                            Term t)
                    throws SLTranslationException {
                return TB.ex(qv, t);
            }
            @Override
            public Term combineQuantifiedTerms(Term t1,
                                               Term t2)
                    throws SLTranslationException {
                return TB.and(t1, t2);
            }
        });
        translationMethods.put("\\bsum", new JMLTranslationMethod() {
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                SLExpression.class, SLExpression.class,
                                SLExpression.class, KeYJavaType.class,
                                ImmutableList.class, Services.class);
                SLExpression a = (SLExpression) params[0];
                SLExpression b = (SLExpression) params[1];
                SLExpression t = (SLExpression) params[2];
                KeYJavaType declsType = (KeYJavaType) params[3];
                ImmutableList<LogicVariable> declVars =
                        (ImmutableList<LogicVariable>) params[4];
                Services services = (Services)params[5];

                if (!declsType.getJavaType().equals(PrimitiveType.JAVA_INT)) {
                    throw new SLTranslationException("bounded sum variable must be of type int");
                } else if (declVars.size() != 1) {
                    throw new SLTranslationException("bounded sum must declare exactly one variable");
                }
                LogicVariable qv = declVars.head();
                Term resultTerm = TB.bsum(qv, a.getTerm(), b.getTerm(), t.getTerm(), services);
                return new SLExpression(resultTerm, t.getType());
            }
        });
//        translationMethods.put("\\min", new Name("min"));
//        translationMethods.put("\\max", new Name("max"));
//        translationMethods.put("\\num_of", new Name("num_of"));
//        translationMethods.put("\\product", new Name("product"));
        translationMethods.put("\\sum", new JMLBoundedNumericalQuantifierTranslationMethod(){
                @Override
                public Term emptyRangeValue() {
                        return TB.zero(services);
                }
                @Override
                public Term translateBoundedNumericalQuantifier(
                                QuantifiableVariable qv, Term lo, Term hi,
                                Term body) {
                        return TB.bsum(qv, lo, hi, body, services);               } 
        }
        );
        
        // primary expressions
        translationMethods.put("\\invariant_for", new JMLTranslationMethod(){

            /**
             * @param params[0] Services
             * @param params[1] SLExpression giving the object
             */
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class);
                final Services services = (Services) params[0];
                Function inv = services.getJavaInfo().getInv();
                Term obj = ((SLExpression)params[1]).getTerm();
                return new SLExpression(TB.func(inv, TB.heap(services), obj));
            }
            
        });
        
        translationMethods.put("\\indexOf", new JMLTranslationMethod(){

            @Override
            public Object translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class, SLExpression.class);
                final Services services = (Services)params[0];
                final Term seq = ((SLExpression)params[1]).getTerm();
                final Term elem = ((SLExpression)params[2]).getTerm();
                final KeYJavaType inttype = services.getJavaInfo().getPrimitiveKeYJavaType("int");
                return new SLExpression(TB.indexOf(services,seq,elem),inttype);
            }
            
        });
        
        translationMethods.put("\\seq_get", new JMLTranslationMethod(){

            @Override
            public Object translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class, SLExpression.class);
                final Services services = (Services)params[0];
                final Term seq = ((SLExpression)params[1]).getTerm();
                final Term idx = ((SLExpression)params[2]).getTerm();
                return new SLExpression(TB.seqGet(services, Sort.ANY, seq, idx));
            }
            
        });
        
        translationMethods.put("\\seq_concat", new JMLTranslationMethod(){

            @Override
            public Object translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class, SLExpression.class);
                final Services services = (Services)params[0];
                final Term seq1 = ((SLExpression)params[1]).getTerm();
                final Term seq2 = ((SLExpression)params[2]).getTerm();
                final KeYJavaType seqtype = services.getJavaInfo().getPrimitiveKeYJavaType("\\seq");
                return new SLExpression(TB.seqConcat(services, seq1, seq2),seqtype);
            }
            
        });
        
        translationMethods.put("\\contains", new JMLTranslationMethod(){
            // this is a quick hack; to be removed eventually; hopefully there will be support for set ADTs soon, so this will be obsolete

            /** @deprecated */
            @Override
            public Object translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class, SLExpression.class);
                final Services services = (Services)params[0];
                final Term seq = ((SLExpression)params[1]).getTerm();
                final Term elem = ((SLExpression)params[2]).getTerm();
                final LogicVariable i = new LogicVariable(new Name("i"), services.getJavaInfo().getPrimitiveKeYJavaType("int").getSort());
                final Term body = TB.and(TB.leq(TB.zero(services), TB.var(i), services),TB.lt(TB.var(i), TB.seqLen(services, seq), services), TB.equals(TB.seqGet(services, Sort.ANY, seq, TB.var(i)), elem));
                return new SLExpression(TB.ex(i, body));
            }
            
        });
        
        translationMethods.put("\\not_modified", new JMLPostExpressionTranslationMethod(){

            @Override
            protected String name() {
                return "\\not_modified";
            }

            /**
             * @param services Services
             * @param heapAtPre The pre-state heap (since we are in a post-condition)
             * @param params Must be of length 1 with a Term (store-ref expression)
             */
            @Override
            protected Term translate(Services services, Term heapAtPre, Object[] params) throws SLTranslationException {
                checkParameters(params, Term.class);
                Term t = (Term) params[0];
                
                // collect variables from storereflist
                java.util.List<Term> storeRefs = new java.util.ArrayList<Term>();
                final LocSetLDT ldt = services.getTypeConverter().getLocSetLDT();
                final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
                while (t.op() == ldt.getUnion()){
                    storeRefs.add(t.sub(0));
                    t = t.sub(1);
                }
                storeRefs.add(t);
                // construct equality predicates
                Term res = TB.tt();
                for (Term sr: storeRefs){
                    if (sr.op() == ldt.getSingleton()){
                        final Term ref = TB.dot(services, Sort.ANY, sr.sub(0), sr.sub(1));
                        res = TB.and(res, TB.equals(ref,convertToOld(services, heapAtPre, ref)));
                    } else if (sr.op() == ldt.getEmpty()){
                        // do nothing
                    } else if (sr.op().equals(ldt.getSetMinus()) && sr.sub(0).op().equals(ldt.getAllLocs()) && sr.sub(1).op().equals(ldt.getFreshLocs())){
                        // this is the case for "\everything"
                        final JavaInfo ji = services.getJavaInfo();
                        final LogicVariable fld = new LogicVariable(new Name("f"), heapLDT.getFieldSort());
                        final LogicVariable obj = new LogicVariable(new Name("o"), ji.objectSort());
                        final Term objTerm = TB.var(obj);
                        final Term fldTerm = TB.var(fld);
                        final Term ref = TB.dot(services, Sort.ANY, objTerm, fldTerm);
                        final Term fresh = TB.subset(services, TB.singleton(services, objTerm, fldTerm ), TB.freshLocs(services, heapAtPre));
                        final Term bodyTerm = TB.or(TB.equals(ref, convertToOld(services, heapAtPre, ref)),fresh);
                        res = TB.and(res, TB.all(fld, TB.all(obj, bodyTerm)));
                    } else {
                        // all other results are not meant to occur
                        throw new SLTranslationException("Term "+sr+" is not a valid store-ref expression.");
                    }
                }
                return res;
            }

        });
        
        translationMethods.put("reach", new JMLFieldAccessExpressionTranslationMethod(){
            @Override
            public Object translate(Object... params) throws SLTranslationException {
                checkParameters(params, Term.class, SLExpression.class, SLExpression.class, SLExpression.class, Services.class);
                final Term t = (Term)params[0];
                final SLExpression e1 = (SLExpression)params[1];
                final SLExpression e2 = (SLExpression)params[2];
                final SLExpression e3 = (SLExpression)params[3];
                final Services services = (Services)params[4];
                final LogicVariable stepsLV = e3 == null ? 
                        new LogicVariable(new Name("n"), services.getTypeConverter().getIntegerLDT().targetSort()) 
                        : null;
                final Term h = TB.heap(services);
                final Term s = getFields(t, services);
                final Term o = e1.getTerm();
                final Term o2 = e2.getTerm();
                final Term n = e3 == null ? TB.var(stepsLV) : e3.getTerm();
                Term reach = TB.reach(services, h, s, o, o2, n);
                if(e3 == null) {
                    reach = TB.ex(stepsLV, reach);
                }
                return new SLExpression(reach);
            }});
        
        translationMethods.put("reachLocs", new JMLFieldAccessExpressionTranslationMethod(){
            @Override
            public Object translate(Object... params)
            throws SLTranslationException {
                checkParameters(params, Term.class, SLExpression.class, SLExpression.class, Services.class);
                final Term t = (Term)params[0];
                final SLExpression e1 = (SLExpression)params[1];
                final SLExpression e3 = (SLExpression)params[2];
                final Services services = (Services)params[3];
                final LogicVariable objLV
                    = new LogicVariable(new Name("o"), services.getJavaInfo().objectSort());
                final LogicVariable stepsLV = e3 == null ? 
                        new LogicVariable(new Name("n"), services.getTypeConverter().getIntegerLDT().targetSort()) 
                        : null;
                final Term h = TB.heap(services);
                final Term s = getFields(t, services);
                final Term o = e1.getTerm();
                final Term o2 = TB.var(objLV);
                final Term n = e3 == null ? TB.var(stepsLV) : e3.getTerm();
                Term reach = TB.reach(services, h, s, o, o2, n);
                if(e3 == null) {
                    reach = TB.ex(stepsLV, reach);
                }

                final LogicVariable fieldLV
                = new LogicVariable(new Name("f"), services.getTypeConverter().getHeapLDT().getFieldSort());
                final Term locSet 
                = TB.guardedSetComprehension(services, 
                        new LogicVariable[]{objLV, fieldLV},
                        reach, 
                        o2,
                        TB.var(fieldLV));

                return new SLExpression(locSet, services.getJavaInfo().getPrimitiveKeYJavaType(PrimitiveType.JAVA_LOCSET));
            }

        });

        // operators
        translationMethods.put("<==>", new JMLEqualityTranslationMethod() {
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                SLExpression.class, SLExpression.class,
                                SLTranslationExceptionManager.class,
                                Services.class);
                SLExpression expr1 = (SLExpression) params[0];
                SLExpression expr2 = (SLExpression) params[1];
                SLTranslationExceptionManager excManager =
                        (SLTranslationExceptionManager) params[2];
                Services services = (Services) params[3];

                checkSLExpressions(expr1, expr2, excManager, "<==>");
                return buildEqualityTerm(expr1, expr2, excManager, services);
            }
        });
        translationMethods.put("<=!=>", new JMLEqualityTranslationMethod() {
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                SLExpression.class, SLExpression.class,
                                SLTranslationExceptionManager.class,
                                Services.class);
                SLExpression expr1 = (SLExpression) params[0];
                SLExpression expr2 = (SLExpression) params[1];
                SLTranslationExceptionManager excManager =
                        (SLTranslationExceptionManager) params[2];
                Services services = (Services) params[3];

                checkSLExpressions(expr1, expr2, excManager, "<=!=>");
                SLExpression eq =
                        buildEqualityTerm(expr1, expr2, excManager, services);
                return new SLExpression(TB.not(eq.getTerm()), eq.getType());
            }
        });
        translationMethods.put("==", new JMLEqualityTranslationMethod() {
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                SLExpression.class, SLExpression.class,
                                SLTranslationExceptionManager.class,
                                Services.class);
                SLExpression expr1 = (SLExpression) params[0];
                SLExpression expr2 = (SLExpression) params[1];
                SLTranslationExceptionManager excManager =
                        (SLTranslationExceptionManager) params[2];
                Services services = (Services) params[3];

                checkSLExpressions(expr1, expr2, excManager, "==");
                return buildEqualityTerm(expr1, expr2, excManager, services);
            }
        });
        translationMethods.put("!=", new JMLEqualityTranslationMethod() {
            @Override
            public SLExpression translate(Object... params)
                    throws SLTranslationException {
                checkParameters(params,
                                SLExpression.class, SLExpression.class,
                                SLTranslationExceptionManager.class,
                                Services.class);
                SLExpression expr1 = (SLExpression) params[0];
                SLExpression expr2 = (SLExpression) params[1];
                SLTranslationExceptionManager excManager =
                        (SLTranslationExceptionManager) params[2];
                Services services = (Services) params[3];

                checkSLExpressions(expr1, expr2, excManager, "!=");
                SLExpression eq =
                        buildEqualityTerm(expr1, expr2, excManager, services);
                if (eq.getType() != null) {
                    return new SLExpression(TB.not(eq.getTerm()), eq.getType());
                } else {
                    return new SLExpression(TB.not(eq.getTerm()));
                }
            }
        });
        
        translationMethods.put("(* *)", new JMLTranslationMethod() {
            public Object translate(Object... params) throws SLTranslationException {

                checkParameters(params, Services.class, Token.class,
                        LocationVariable.class, LocationVariable.class, 
                        ImmutableList.class, Term.class, 
                        SLTranslationExceptionManager.class);
                
                Services services = (Services) params[0];
                Token desc = (Token) params[1]; 
                LocationVariable selfVar = (LocationVariable) params[2];
                LocationVariable resultVar = (LocationVariable) params[3];
                ImmutableList<LocationVariable> paramVars = 
                    (ImmutableList<LocationVariable>) params[4];
                Term heapAtPre = (Term) params[5];
                SLTranslationExceptionManager excMan = 
                    (SLTranslationExceptionManager) params[6];
                
                // strip leading and trailing (* ... *)
                String text = desc.getText();
                text = text.substring(2, text.length() - 2);
                
                // prepare namespaces
                NamespaceSet namespaces = services.getNamespaces().copy();
                Namespace programVariables = namespaces.programVariables();

                if(heapAtPre != null && heapAtPre.op() instanceof ProgramVariable) {
                    programVariables.add(heapAtPre.op());
                }

                if(selfVar != null) {
                    programVariables.add(selfVar);
                }

                if(resultVar != null) {
                    programVariables.add(resultVar);
                }

                if(paramVars != null) {
                    for (ProgramVariable param : paramVars) {
                        programVariables.add(param);
                    }
                }

                SLExpression result;
                try {
                    result = new SLExpression(TB.parseTerm(text, services, namespaces));
                    return result;
                } catch (ParserException ex) {
                    throw excMan.createException("Cannot parse embedded JavaDL: " + text, desc, ex);
                }
            }
        });
        
        translationMethods.put("\\dl_", new JMLTranslationMethod() {
            @Override
            public Object translate(Object... params) throws SLTranslationException {
                checkParameters(params, Token.class, ImmutableList.class, Services.class,
                        SLTranslationExceptionManager.class);
                
                Token escape = (Token) params[0];
                ImmutableList<SLExpression> list = (ImmutableList<SLExpression>) params[1];
                Services services = (Services) params[2];
                SLTranslationExceptionManager excMan = (SLTranslationExceptionManager) params[3];

                // strip leading "\dl_"
                String functName = escape.getText().substring(4);
                Namespace funcs = services.getNamespaces().functions();
                Named symbol = funcs.lookup(new Name(functName));
                
                if(symbol != null) {
                    // Function symbol found

                    assert symbol instanceof Function : "Expecting a function symbol in this namespace";
                    Function function = (Function) symbol;
                    
                    Term[] args;
                    if(list == null) {
                        // empty parameter list
                        args = new Term[0];
                    } else {

                        Term heap = TB.heap(services);

                        // special casing "implicit heap" arguments:
                        // omitting one argument means first argument is "heap"
                        int i = 0;
                        if(function.arity() == list.size() + 1 
                                && function.argSort(0) == heap.sort()) {
                            args = new Term[list.size() + 1];
                            args[i++] = heap;
                        } else {
                            args = new Term[list.size()];
                        }

                        for (SLExpression expr : list) {
                            if(!expr.isTerm()) {
                                throw new SLTranslationException("Expecting a term here, not: " + expr);
                            }
                            args[i++] = expr.getTerm();
                        }
                    }

                    try {
                        Term resultTerm = TB.func(function, args, null);
                        SLExpression result = new SLExpression(resultTerm);
                        return result;
                    } catch (TermCreationException ex) {
                        throw excMan.createException("Cannot create term " + function.name() + 
                                "(" + MiscTools.join(args, ", ") + ")", escape, ex);
                    }
                    
                }

                assert symbol == null;  // no function symbol found
                
                Namespace progVars = services.getNamespaces().programVariables();
                symbol = progVars.lookup(new Name(functName));
                
                if(symbol == null) {
                    throw excMan.createException("Unknown escaped symbol " + functName, escape);
                }
                
                assert symbol instanceof ProgramVariable : "Expecting a program variable";
                ProgramVariable pv = (ProgramVariable)symbol;
                try {
                    Term resultTerm = TB.var(pv);
                    SLExpression result = new SLExpression(resultTerm);
                    return result; 
                } catch (TermCreationException ex) {
                    throw excMan.createException("Cannot create term " + pv.name(), escape, ex);
                }
                
            }});

        // others
        translationMethods.put("array reference", new JMLTranslationMethod(){

            @Override
            public Object translate(Object... params)
            throws SLTranslationException {
                checkParameters(params, Services.class, SLExpression.class, String.class, Token.class, SLExpression.class, SLExpression.class);
                Services services = (Services)params[0];
                SLExpression receiver = (SLExpression)params[1];
                String fullyQualifiedName = (String)params[2];
                Token lbrack = (Token)params[3];
                SLExpression rangeFrom = (SLExpression)params[4];
                SLExpression rangeTo = (SLExpression)params[5];
                SLExpression result = null;
                try{
                    whatToDoFirst(receiver, fullyQualifiedName, lbrack);

                    //arrays
                    if(receiver.getType().getJavaType() instanceof ArrayType) {
                        result = translateArrayReference(services, receiver,
                                rangeFrom, rangeTo);

                        //sequences 
                    } else {
                        result = translateSequenceReference(services, receiver,
                                rangeFrom, rangeTo);   
                    }
                    return result;
                }
                catch (TermCreationException tce){
                    raiseError(tce.getMessage());
                    return null;
                }}

            private void whatToDoFirst(SLExpression receiver,
                    String fullyQualifiedName, Token lbrack)
                    throws SLTranslationException {
                if(receiver == null) {
                    raiseError("Array \"" + fullyQualifiedName + "\" not found.", lbrack);
                } else if(receiver.isType()) {
                    raiseError("Error in array expression: \"" + fullyQualifiedName +
                            "\" is a type.", lbrack);
                } else if(!(receiver.getType().getJavaType() instanceof ArrayType
                        || receiver.getType().getJavaType().equals(PrimitiveType.JAVA_SEQ))) {
                    raiseError("Cannot access " + receiver.getTerm() + " as an array.");
                }
            }

            private SLExpression translateArrayReference(Services services,
                    SLExpression receiver, SLExpression rangeFrom,
                    SLExpression rangeTo) {
                SLExpression result;
                if (rangeFrom == null) {
                    // We have a star. A star includes all components of an array even
                    // those out of bounds. This makes proving easier.      
                    Term t = TB.allFields(services, receiver.getTerm());
                    result = new SLExpression(t);
                } else if (rangeTo != null) {
                    // We have "rangeFrom .. rangeTo"
                    Term t = TB.arrayRange(services, 
                            receiver.getTerm(), 
                            rangeFrom.getTerm(), 
                            rangeTo.getTerm());
                    result = new SLExpression(t);
                } else {
                    // We have a regular array access
                    Term t = TB.dotArr(services, 
                            receiver.getTerm(),
                            rangeFrom.getTerm());
                    ArrayType arrayType = (ArrayType) receiver.getType().getJavaType();
                    KeYJavaType elementType = arrayType.getBaseType().getKeYJavaType();                    
                    result = new SLExpression(t, elementType);
                }
                return result;
            }

            private SLExpression translateSequenceReference(Services services,
                    SLExpression receiver, SLExpression rangeFrom,
                    SLExpression rangeTo) throws SLTranslationException {
                if (rangeFrom == null){
                    // a star
                    return new SLExpression(TB.allFields(services, receiver.getTerm()));
                } else
                    if(rangeTo != null) {
                        Term t = TB.seqSub(services, 
                                receiver.getTerm(), 
                                rangeFrom.getTerm(), 
                                rangeTo.getTerm());
                        return new SLExpression(t);
                    } else {
                        Term t = TB.seqGet(services, 
                                Sort.ANY,
                                receiver.getTerm(), 
                                rangeFrom.getTerm());
                        return new SLExpression(t);
                    }
            }

        });
    }



    //-------------------------------------------------------------------------
    // public methods
    //-------------------------------------------------------------------------


    public static JMLTranslator getInstance() {
        return instance;
    }
    
    public void setExceptionManager (SLTranslationExceptionManager sltem){
        excManager = sltem;
    }


    @SuppressWarnings("unchecked")
    public <T> T translate(String jmlKeyword,
                          Object... params)
            throws SLTranslationException {
        JMLTranslationMethod m = translationMethods.get(jmlKeyword);
        if (m != null) {
            Object result = m.translate(params);
            this.<T>checkReturnType(result);
            return (T) result;
        } else {
            throw new SLTranslationException(
                    "Unknown translation for JML-keyword \""
                    + jmlKeyword
                    + "\". The keyword seems not to be supported yet.");
        }
    }


    /**
     *
     */
    @SuppressWarnings("unchecked")
    public <T> T parse(PositionedString expr,
                       KeYJavaType specInClass,
                       ProgramVariable selfVar,
                       ImmutableList<ProgramVariable> paramVars,
                       ProgramVariable resultVar,
                       ProgramVariable excVar,
                       Term heapAtPre,
                       Services services)
            throws SLTranslationException {
        final KeYJMLParser parser = new KeYJMLParser(expr, services,
                                                     specInClass, selfVar,
                                                     paramVars, resultVar,
                                                     excVar, heapAtPre);
        Object result = null;
	try {
	    result = parser.top();
	} catch (antlr.ANTLRException e) {
	    throw parser.getExceptionManager().convertException(e);
	}
        this.<T>checkReturnType(result);
        return (T) result;
    }
    

    //-------------------------------------------------------------------------
    // private methods
    //-------------------------------------------------------------------------


    private void checkParameters(Object[] params,
                                 Class... classes)
            throws SLTranslationException {
        boolean ok = true;
        int i = 0;
        while (i < params.length && i < classes.length && ok) {
            ok &= params[i] == null || classes[i].isInstance(params[i]);
            i++;
        }
        if (!ok) {
            throw new SLTranslationException(
                    "Parameter " + i + " does not match the expected type.\n"
                     + "Parameter type was: " + params[i - 1].getClass().getName()
                     + "\nExpected type was:  " + classes[i - 1].getName());
        } else if (i < classes.length) {
            throw new SLTranslationException(
                    "Parameter" + i + " is missing. The expected type is \""
                    + classes[i].toString() + "\".");
        } else if (i < params.length) {
            throw new SLTranslationException(
                    (params.length - i) + " more parameters than expected.");
        }
    }


    @SuppressWarnings("unchecked")
    private <T> void checkReturnType(Object result)
            throws SLTranslationException {
        try {
            // TODO This is not type-safe. Implement this with a Class-argument.
            result = (T) result;
        } catch (ClassCastException e) {
            throw new SLTranslationException(
                    "Return value does not match the expected return type:\n"
                    + "Return type was: " + result.getClass() + "\n"
                    + "Tried conversion was: " + e.getMessage());
        }
    }

    private void raiseError(String msg) throws SLTranslationException {
        if (excManager == null)
            throw new SLTranslationException(msg);
        else
            throw excManager.createException(msg);
    }


    private void raiseError(String msg, Token t) throws SLTranslationException {
        if (excManager == null)
            throw new SLTranslationException(msg);
        else
            throw excManager.createException(msg, t);   
    }


    @SuppressWarnings("unused")
    private void raiseNotSupported(String feature) 
    throws SLTranslationException {
        if (excManager == null)
            throw new SLTranslationException(feature + " not supported");
        else
            throw excManager.createWarningException(feature + " not supported"); 
    }

    /**
     * This is used for features without semantics such as labels or annotations.
     * @author bruns
     * @since 1.7.2178
     */
    @SuppressWarnings("unused")
    private void addIgnoreWarning(String feature) {
        String msg = feature + " is not supported and has been silently ignored.";
        // TODO: wasn't there some collection of non-critical warnings ???
    }


    //-------------------------------------------------------------------------
    // private classes
    //-------------------------------------------------------------------------

    
    private abstract class JMLQuantifierTranslationMethod implements
            JMLTranslationMethod {
            
            protected Services services;


        /**
         * Add implicit "non-null" and "created" guards for reference types,
         * "in-bounds" guards for integer types. Then, translateToTerm the quantifier.
         * @param quantName
         * @param declVars
         * @param expr
         * @param preTerm
         * @param bodyTerm
         * @param nullable
         * @param services
         * @return
         * @throws SLTranslationException
         */
        @SuppressWarnings("unchecked")
        @Override
        public Term translate(Object... params)
                throws SLTranslationException {
            checkParameters(params,
                            Term.class, Term.class, KeYJavaType.class,
                            ImmutableList.class, Boolean.class, Services.class);
            Term preTerm = (Term) params[0];
            Term bodyTerm = (Term) params[1];
            KeYJavaType declsType = (KeYJavaType) params[2];
            ImmutableList<LogicVariable> declVars =
                    (ImmutableList<LogicVariable>) params[3];
            boolean nullable = (Boolean) params[4];
            services = (Services) params[5];

            Term nullTerm = TB.NULL(services);
            for (LogicVariable lv : declVars) {
                preTerm = TB.and(preTerm,
                                 TB.reachableValue(services, TB.var(lv),
                                                   declsType));
                if (lv.sort().extendsTrans(services.getJavaInfo().objectSort())
                    && !nullable) {
                    preTerm = TB.and(preTerm, TB.not(TB.equals(TB.var(lv),
                                                               nullTerm)));
                }
            }

            return translateQuantifiers(declVars, preTerm, bodyTerm);
        }


        public Term translateQuantifiers(Iterable<LogicVariable> qvs,
                                         Term t1,
                                         Term t2)
                throws SLTranslationException {
            Term result = combineQuantifiedTerms(t1, t2);
            for (LogicVariable qv : qvs) {
                result = translateQuantifier(qv, result);
            }
            return result;
        }

        public abstract Term combineQuantifiedTerms(Term t1,
                                                    Term t2)
                throws SLTranslationException;

        public abstract Term translateQuantifier(QuantifiableVariable qv,
                                                 Term t)
                throws SLTranslationException;
     
    }
    
    /**
     * Abstract super-class for translation methods which enumerate fields such as <code>\reach</code>.
     * @author bruns
     *
     */
    private abstract class JMLFieldAccessExpressionTranslationMethod implements JMLTranslationMethod {
        
        /**
         * Creates an "all-objects" term from a store-ref term.
         * @param t store-ref term, needs to be a union of singletons
         * @param services
         * @return allObjects term (see <code>LocSetADT</code>)
         * @throws SLTranslationException in case <code>t</code> is not a store-ref term cosisting of unions of singletons
         */
        protected Term getFields(Term t, Services services) throws SLTranslationException {
            final LocSetLDT locSetLDT = services.getTypeConverter().getLocSetLDT();
            if(t.op().equals(locSetLDT.getUnion())) {
                final Term sub0 = getFields(t.sub(0),services);
                final Term sub1 = getFields(t.sub(1),services);
                return TB.union(services, sub0, sub1);
            } else if(t.op().equals(locSetLDT.getSingleton())) {
            return TB.allObjects(services, t.sub(1));
            } else {
                raiseError("Inacceptable field expression: " + t);
                return null;
            }
        }
    }

    private abstract class JMLBoundedNumericalQuantifierTranslationMethod extends JMLQuantifierTranslationMethod {
            final static String notBounded = "Only numerical quantifier expressions of form (\\sum int i; l<=i && i<u; t) are permitted";
            final static String notInt = "Bounded numerical quantifier variable must be of type int.";


            private  boolean isBoundedNumerical(Term a, LogicVariable lv){
                    return lowerBound(a,lv)!=null && upperBound(a,lv)!=null;
            }

            /**
             * Extracts lower bound from <code>a</code> if it matches the pattern.
             * @param a guard to be disected
             * @param lv variable bound by quantifier
             * @return lower bound term (or null)
             */
            private  Term lowerBound(Term a, LogicVariable lv){
                    if(a.arity()>0 && a.sub(0).op()==Junctor.AND){
                            a=a.sub(0);
                    }
                    if(a.arity()==2 && a.op()== Junctor.AND && a.sub(0).arity()==2 && a.sub(0).sub(1).op()==lv
                                    && a.sub(0).op().equals(services.getTypeConverter().getIntegerLDT().getLessOrEquals())){
                            return a.sub(0).sub(0);
                    }
                    return null;
            }

            /**
             * Extracts upper bound from <code>a</code> if it matches the pattern.
             * @param a guard to be disected
             * @param lv variable bound by quantifier
             * @return upper bound term (or null)
             */
            private Term upperBound(Term a, LogicVariable lv){
                    if(a.arity()>0 && a.sub(0).op()==Junctor.AND){
                            a=a.sub(0);
                    }   
                    if(a.arity()==2 && a.op()==Junctor.AND && a.sub(1).arity()==2 && a.sub(1).sub(0).op()==lv
                                    && a.sub(1).op().equals(services.getTypeConverter().getIntegerLDT().getLessThan())){
                            return a.sub(1).sub(1);
                    }
                    return null;
            }


            @Override
            public Term translate(Object... params)
            throws SLTranslationException {
                    checkParameters(params,
                                    Term.class, Term.class, KeYJavaType.class,
                                    ImmutableList.class, Boolean.class, Services.class);
                    KeYJavaType declsType = (KeYJavaType) params[2];
                    if (!declsType.getJavaType().equals(PrimitiveType.JAVA_INT))
                            throw new SLTranslationException(notInt);
                    return super.translate(params);
            }

            @Override
            public Term translateQuantifiers(Iterable<LogicVariable> qvs, Term t1, Term t2)
            throws SLTranslationException {
                    Iterator<LogicVariable> it = qvs.iterator();
                    LogicVariable lv = it.next();
                    if (it.hasNext() || !isBoundedNumerical(t1, lv)){
                            throw new SLTranslationException(notBounded);
                    } else {
                            if (t1.arity()>0 && t1.sub(0).op()==Junctor.AND)
                                    t2 = TB.ife(t1.sub(1), t2, emptyRangeValue());
                            return translateBoundedNumericalQuantifier(lv, lowerBound(t1, lv), upperBound(t1, lv), t2);
                    }
            }

            /** Creates a term for a bounded numerical quantifier (e.g., sum).*/
            public abstract Term translateBoundedNumericalQuantifier(QuantifiableVariable qv, Term lo, Term hi, Term body);

            /** Gives the defined term for an empty range to quantify over (e.g., zero for sum). */
            public abstract Term emptyRangeValue ();

            /** Should not be called. */
            @Override
            @Deprecated
            public Term combineQuantifiedTerms(Term t1, Term t2){
                    assert false;
                    return null;
            }
            /** Should not be called. */
            @Override
            @Deprecated
            public Term translateQuantifier(QuantifiableVariable qv,
                            Term t){
                    assert false;
                    return null;
            }
    }
    
    /**
     * Translation method for expressions only allowed to appear in a postcondition.
     * @author bruns
     *
     */
    private abstract class JMLPostExpressionTranslationMethod implements JMLTranslationMethod {

        protected void assertPost (Term heapAtPre) throws SLTranslationException{
            if (heapAtPre == null){
                throw new SLTranslationException("JML construct "+name()+" not allowed in this context.");
            }
        }
        

        /**
         * Converts a term so that all of its non-rigid operators refer to the pre-state.
         */
        protected Term convertToOld(Services services, Term heapAtPre, Term term) {
            assert heapAtPre != null;
            Map<Term,Term> map = new LinkedHashMap<Term, Term>();
            map.put(TB.heap(services), heapAtPre);
            OpReplacer or = new OpReplacer(map);
            return or.replace(term);
        }

        /**
         * Name of this translation method;
         */
        protected abstract String name();

        protected abstract Term translate (Services services, Term heapAtPre, Object[] params) throws SLTranslationException;

        public Term translate (Object ... params) throws SLTranslationException{
            if (!(params[0] instanceof Services && params[1] instanceof Term))
                throw new SLTranslationException(
                        "Parameter 2 does not match the expected type.\n"
                        + "Parameter type was: " + params[1].getClass().getName()
                        + "\nExpected type was:  Term");
            Term heapAtPre = (Term) params[1];
            assertPost(heapAtPre);
            return translate((Services)params[0], heapAtPre, Arrays.copyOfRange(params, 1, params.length-1));
        }
    }


    private abstract class JMLEqualityTranslationMethod implements
            JMLTranslationMethod {

        protected void checkSLExpressions(SLExpression expr1,
                                          SLExpression expr2,
                                          SLTranslationExceptionManager excManager,
                                          String eqSymb)
        throws SLTranslationException {
            if (expr1.isType() != expr2.isType()) {
                throw excManager.createException(
                        "Cannot build equality expression (" + eqSymb
                        + ") between term and type.");
            }

        }


        protected SLExpression buildEqualityTerm(SLExpression a,
                                                 SLExpression b,
                                                 SLTranslationExceptionManager excManager,
                                                 Services services)
                throws SLTranslationException {

            if (a.isTerm() && b.isTerm()) {
                return new SLExpression(buildEqualityTerm(a.getTerm(),
                                                          b.getTerm(),
                                                          excManager,
                                                          services));
            }

            if (a.isType() && b.isType()) {
                SLExpression typeofExpr;
                SLExpression typeExpr;
                if (a.getTerm() != null) {
                    typeofExpr = a;
                    typeExpr = b;
                } else {
                    if (b.getTerm() == null) {
                        throw excManager.createException(
                                "Type equality only supported for expressions "
                                + " of shape \"\\typeof(term) == \\type(Typename)\"");
                    }
                    typeofExpr = b;
                    typeExpr = a;
                }

                Sort os = typeExpr.getType().getSort();
                Function ioFunc = os.getExactInstanceofSymbol(services);

                return new SLExpression(TB.equals(
                        TB.func(ioFunc, typeofExpr.getTerm()),
                        TB.TRUE(services)));
            }

            // this should not be reached
            throw excManager.createException("Equality must be between two terms or " +
            		"two formulas, not term and formula.");
        }


        protected Term buildEqualityTerm(Term a,
                                         Term b,
                                         SLTranslationExceptionManager excManager1,
                                         Services services)
                throws SLTranslationException {

            Term result = null;
            try {
                if (a.sort() != Sort.FORMULA && b.sort() != Sort.FORMULA) {
                    result = TB.equals(a, b);
                } else {
                    result = TB.equals(TB.convertToFormula(a, services),
                                       TB.convertToFormula(b, services));
                }
                return result;
            } catch (IllegalArgumentException e) {
                throw excManager1.createException(
                        "Illegal Arguments in equality expression.");
                //"near " + LT(0));
            } catch (TermCreationException e) {
                throw excManager1.createException("Error in equality-expression\n"
                                           + a.toString() + " == "
                                           + b.toString() + ".");
            }
        }
    }
}