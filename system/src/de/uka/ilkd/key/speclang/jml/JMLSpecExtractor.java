// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang.jml;

import de.uka.ilkd.key.collection.*;
import de.uka.ilkd.key.java.Comment;
import de.uka.ilkd.key.java.Position;
import de.uka.ilkd.key.java.ProgramElement;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.ArrayType;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.abstraction.Type;
import de.uka.ilkd.key.java.declaration.*;
import de.uka.ilkd.key.java.declaration.modifier.VisibilityModifier;
import de.uka.ilkd.key.java.recoderext.JMLTransformer;
import de.uka.ilkd.key.java.reference.TypeReference;
import de.uka.ilkd.key.java.statement.LoopStatement;
import de.uka.ilkd.key.logic.op.ProgramMethod;
import de.uka.ilkd.key.speclang.*;
import de.uka.ilkd.key.speclang.jml.pretranslation.*;
import de.uka.ilkd.key.speclang.jml.translation.JMLSpecFactory;
import de.uka.ilkd.key.speclang.translation.SLTranslationException;
import de.uka.ilkd.key.speclang.translation.SLWarningException;

/**
 * Extracts JML class invariants and operation contracts from JML comments. 
 * This is the public interface to the jml package. Note that internally,
 * this class is highly similar to the class java.recoderext.JMLTransformer; 
 * if you change one of these classes, you probably need to change the other 
 * as well.
 */
public final class JMLSpecExtractor implements SpecExtractor {

    private final Services services;
    private final JMLSpecFactory jsf;
    private ImmutableSet<PositionedString> warnings 
        = DefaultImmutableSet.<PositionedString>nil();

    
    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------

    public JMLSpecExtractor(Services services) {
        this.services = services;
        this.jsf = new JMLSpecFactory(services);
    }

    
    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------

    /**
     * Concatenates the passed comments in a position-preserving way. (see also
     * JMLTransformer::concatenate(), which does the same thing for Recoder
     * ASTs)
     */
    private String concatenate(Comment[] comments) {
        if(comments.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer(comments[0].getText());

        for(int i = 1; i < comments.length; i++) {
            Position relativePos = comments[i].getRelativePosition();
            for(int j = 0; j < relativePos.getLine(); j++) {
                sb.append("\n");
            }
            for(int j = 0; j < relativePos.getColumn(); j++) {
                sb.append(" ");
            }
            sb.append(comments[i].getText());
        }

        return sb.toString();
    }

    
    private int getIndexOfMethodDecl(ProgramMethod pm,
                                     TextualJMLConstruct[] constructsArray) {
        for(int i = 0; i < constructsArray.length; i++) {
            if(constructsArray[i] instanceof TextualJMLMethodDecl) {
                TextualJMLMethodDecl methodDecl 
                    = (TextualJMLMethodDecl) constructsArray[i];
                if(methodDecl.getMethodName().equals(pm.getName())) {
                    return i;
                }
            }
        }
        return -1;
    }

    
 
    private String getDefaultSignalsOnly(ProgramMethod pm) {
        if(pm.getThrown() == null) {
            return "signals_only \\nothing;";
        }

        ImmutableArray<TypeReference> exceptions = pm.getThrown().getExceptions();

        if(exceptions == null) {
            return "signals_only \\nothing;";
        }

        String exceptionsString = "";

        for(int i = 0; i < exceptions.size(); i++) {
            //only subtypes of java.lang.Exception are in the default
            //signals-only
            if(services.getJavaInfo().isSubtype(
                    exceptions.get(i).getKeYJavaType(),
                    services.getJavaInfo()
                            .getKeYJavaType("java.lang.Exception"))) {
                exceptionsString 
                    += exceptions.get(i).getName() + ", ";
            }
        }

        if(exceptionsString.equals("")) {
            exceptionsString = "\\nothing";
        } else {
            //delete the last ", "
            exceptionsString 
                = exceptionsString.substring(0, exceptionsString.length() - 2);
        }
        return "signals_only " + exceptionsString + ";";
    }

    
    /**
     * creates a JML specification expressing that the given variable/field is not null and in case of a reference
     * array type that also its elements are non-null 
     * In case of implicit fields or primitive typed fields/variables the empty set is returned 
     * @param varName the String specifying the variable/field name
     * @param kjt the KeYJavaType representing the variables/field declared type
     * @param isImplicitVar a boolean indicating if the the field is an implicit one (in which case no 
     * @param fileName the String containing the filename where the field/variable has been declared
     * @param pos the Position where to place this implicit specification
     * @return set of formulas specifying non-nullity for field/variables
     */  
    public static ImmutableSet<PositionedString> createNonNullPositionedString(String varName, KeYJavaType kjt, 
	    boolean isImplicitVar, String fileName, Position pos, Services services) {
	ImmutableSet<PositionedString> result = DefaultImmutableSet.<PositionedString>nil(); 
	final Type varType  = kjt.getJavaType(); 

	if (services.getTypeConverter().isReferenceType(varType) && !isImplicitVar) {

	    PositionedString ps 
	    = new PositionedString(varName + " != null", fileName, pos);
	    result = result.add(ps);
	    if (varType instanceof ArrayType && 
		    services.getTypeConverter().
		    isReferenceType(((ArrayType)varType).getBaseType().getKeYJavaType())) {
		final PositionedString arrayElementsNonNull 
		= new PositionedString("(\\forall int i; 0 <= i && i < " + varName + ".length;"
			+ varName + "[i]" + " != null)", 
			fileName, 
			pos);
		result = result.add(arrayElementsNonNull);
	    }
	}
	return result;
    }
    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------

    @Override
    public ImmutableSet<SpecificationElement> extractClassSpecs(KeYJavaType kjt)
            throws SLTranslationException {
        ImmutableSet<SpecificationElement> result 
        	= DefaultImmutableSet.<SpecificationElement>nil();

        //primitive types have no class invariants
        if(!(kjt.getJavaType() instanceof TypeDeclaration)) {
            return result;
        }

        //get type declaration, file name
        TypeDeclaration td = (TypeDeclaration) kjt.getJavaType();
        String fileName = td.getPositionInfo().getFileName();

        //add invariants for non_null fields        
        for(MemberDeclaration member : td.getMembers()) {
            if(member instanceof FieldDeclaration) {
        	VisibilityModifier visibility = null;
        	for(Modifier mod : member.getModifiers()) {
        	    if(mod instanceof VisibilityModifier) {
        		visibility = (VisibilityModifier)mod;
        		break;
        	    }
        	}
                for(FieldSpecification field 
                      : ((FieldDeclaration) member).getFieldSpecifications()) {
                    
                    //add invariant only for fields of reference types
                    //and not for implicit fields.
                    if(!JMLInfoExtractor.isNullable(field.getProgramName(), kjt)) {
                	ImmutableSet<PositionedString> nonNullInvs =
                	    createNonNullPositionedString(field.getProgramName(),
                		    field.getProgramVariable().getKeYJavaType(),
                		    field instanceof ImplicitFieldSpecification,
                		    fileName, member.getEndPosition(),services);
                	for(PositionedString classInv : nonNullInvs) {
                	    result = result.add(jsf.createJMLClassInvariant(kjt,
                		    					    visibility,
                		            				    classInv));
                	}
                    }
                }
            }
        }

        //iterate over all children
        for(int i = 0, n = td.getChildCount(); i <= n; i++) {
            //collect comments
            //(last position are comments of type declaration itself)
            Comment[] comments = null;
            if(i < n) {
                ProgramElement child = td.getChildAt(i);
                comments = child.getComments();
                //skip model and ghost elements
                //(their comments are duplicates of other comments)
                if((child instanceof FieldDeclaration 
                       && (((FieldDeclaration) child).isGhost()
                           || ((FieldDeclaration) child).isModel()))
                    || (child instanceof ProgramMethod 
                        && ((ProgramMethod) child).isModel())) {
                    continue;
                }
            } else if(td.getComments() != null) {
                comments = td.getComments();
            }
            if(comments == null || comments.length == 0) {
                continue;
            }

            //concatenate comments, determine position
            String concatenatedComment = concatenate(comments);
            Position pos = comments[0].getStartPosition();

            //call preparser
            KeYJMLPreParser preParser 
                = new KeYJMLPreParser(concatenatedComment, fileName, pos);
            ImmutableList<TextualJMLConstruct> constructs 
                = preParser.parseClasslevelComment();
            warnings = warnings.union(preParser.getWarnings());

            //create class invs out of textual constructs, add them to result
            for(TextualJMLConstruct c : constructs) {
        	try {        	
        	    if(c instanceof TextualJMLClassInv) {
        		TextualJMLClassInv textualInv = (TextualJMLClassInv) c;
        		ClassInvariant inv 
        			= jsf.createJMLClassInvariant(kjt, textualInv);
        		result = result.add(inv);
        	    } else if(c instanceof TextualJMLInitially) {
        	        TextualJMLInitially textualRep = (TextualJMLInitially) c;
        	        InitiallyClause inc = jsf.createJMLInitiallyClause(kjt, textualRep);
        	        result = result.add(inc);
        	    } else if(c instanceof TextualJMLRepresents) {
        		TextualJMLRepresents textualRep = (TextualJMLRepresents) c;
        		ClassAxiom rep 
        			= jsf.createJMLRepresents(kjt, textualRep);
        		result = result.add(rep);
        	    } else if(c instanceof TextualJMLDepends) {
        		TextualJMLDepends textualDep = (TextualJMLDepends) c;
        		Contract depContract 
        			= jsf.createJMLDependencyContract(kjt, textualDep);
        		result = result.add(depContract);
        	    } else if (c instanceof TextualJMLClassAxiom){
        		ClassAxiom ax = jsf.createJMLClassAxiom(kjt, (TextualJMLClassAxiom)c);
        		result = result.add(ax);
        	    } // else might be some other specification
        	} catch (SLWarningException e) {
        	    warnings = warnings.add(e.getWarning());
        	}
            }
        }

        return result;
    }

    
    @Override    
    public ImmutableSet<SpecificationElement> extractMethodSpecs(ProgramMethod pm)
    throws SLTranslationException {
        ImmutableSet<SpecificationElement> result 
        = DefaultImmutableSet.<SpecificationElement>nil();

        //get type declaration, file name
        TypeDeclaration td 
        = (TypeDeclaration) pm.getContainerType().getJavaType();
        String fileName = td.getPositionInfo().getFileName();

        //determine purity
        final boolean isPure = JMLInfoExtractor.isPure(pm);
        final boolean isHelper = JMLInfoExtractor.isHelper(pm);

        //get textual JML constructs
        Comment[] comments = pm.getComments();
        ImmutableList<TextualJMLConstruct> constructs;
        if(comments.length != 0) {
            //concatenate comments, determine position
            String concatenatedComment = concatenate(comments);
            Position pos = comments[0].getStartPosition();

            //call preparser
            KeYJMLPreParser preParser 
            = new KeYJMLPreParser(concatenatedComment, fileName, pos);
            constructs = preParser.parseClasslevelComment();
            warnings = warnings.union(preParser.getWarnings());
        } else {
            constructs = ImmutableSLList.<TextualJMLConstruct>nil();
        }

        //create JML contracts out of constructs, add them to result
        TextualJMLConstruct[] constructsArray 
        = constructs.toArray(new TextualJMLConstruct[constructs.size()]);

        int startPos;
        if(pm.isModel()) {
            startPos = getIndexOfMethodDecl(pm, constructsArray) - 1;
            assert startPos != constructsArray.length - 1;
        } else {
            startPos = constructsArray.length - 1;
        }
        for(int i = startPos; 
        i >= 0 && constructsArray[i] instanceof TextualJMLSpecCase; 
        i--) {
            TextualJMLSpecCase specCase 
            = (TextualJMLSpecCase) constructsArray[i];

            //add purity
            if(isPure) {
                specCase.addAssignable(new PositionedString("assignable \\nothing"));
            }

            //add invariants
            if(!pm.isStatic() && !isHelper) {
                if(!pm.isConstructor()) {
                    specCase.addRequires(new PositionedString("<inv>"));
                }
                if(specCase.getBehavior() != Behavior.EXCEPTIONAL_BEHAVIOR) {
                    specCase.addEnsures(new PositionedString("ensures <inv>"));
                }
                if(specCase.getBehavior() != Behavior.NORMAL_BEHAVIOR) {
                    specCase.addSignals(new PositionedString("signals (Exception e) <inv>"));
                }
            }

            //add non-null preconditions
            for(int j = 0, n = pm.getParameterDeclarationCount(); j < n; j++) {
                final VariableSpecification paramDecl = 
                        pm.getParameterDeclarationAt(j).getVariableSpecification();
                if (!JMLInfoExtractor.parameterIsNullable(pm, j)) {
                    //no additional precondition for primitive types! createNonNullPos... takes care of that
                    final ImmutableSet<PositionedString> nonNullParams = 
                        createNonNullPositionedString(paramDecl.getName(),
                                paramDecl.getProgramVariable().getKeYJavaType(),
                                false,
                                fileName, pm.getStartPosition(),services);
                    for (PositionedString nonNull : nonNullParams) {
                        specCase.addRequires(nonNull);
                    }
                }
            }

            //add non-null postcondition
            KeYJavaType resultType = pm.getKeYJavaType();

            if(resultType != null &&
                    !JMLInfoExtractor.resultIsNullable(pm) &&
                    specCase.getBehavior() != Behavior.EXCEPTIONAL_BEHAVIOR) {
                final ImmutableSet<PositionedString> resultNonNull = 
                    createNonNullPositionedString("\\result", resultType, false, 
                            fileName, pm.getStartPosition(),services);
                for (PositionedString nonNull : resultNonNull) {
                    specCase.addEnsures(nonNull.prepend("ensures "));
                }               
            }

            //add implicit signals-only if omitted
            if(specCase.getSignalsOnly().isEmpty()
                    && specCase.getBehavior() != Behavior.NORMAL_BEHAVIOR) {
                specCase.addSignalsOnly(
                        new PositionedString(getDefaultSignalsOnly(pm)));
            }

            //translate contract
            try {
                ImmutableSet<Contract> contracts 
                = jsf.createJMLOperationContracts(pm, specCase);
                for(Contract contract : contracts) {
                    result = result.add(contract);
                }
            } catch (SLWarningException e) {
                warnings = warnings.add(e.getWarning());
            }
        }

        return result;
    }

    
    @Override    
    public LoopInvariant extractLoopInvariant(ProgramMethod pm,
                                              LoopStatement loop) 
            throws SLTranslationException {
        LoopInvariant result = null;

        //get type declaration, file name
        TypeDeclaration td 
            = (TypeDeclaration) pm.getContainerType().getJavaType();
        String fileName = td.getPositionInfo().getFileName();

        //get comments
        Comment[] comments = loop.getComments();
        if(comments.length == 0) {
            return result;
        }

        //concatenate comments, determine position
        String concatenatedComment = concatenate(comments);
        Position pos = comments[0].getStartPosition();

        //call preparser
        KeYJMLPreParser preParser 
            = new KeYJMLPreParser(concatenatedComment, fileName, pos);
        ImmutableList<TextualJMLConstruct> constructs 
            = preParser.parseMethodlevelComment();
        warnings = warnings.union(preParser.getWarnings());

        //create JML loop invariant out of last construct
        if(constructs.size() == 0) {
            return result;
        }
        TextualJMLConstruct c = constructs.take(constructs.size() - 1).head();
        if(c instanceof TextualJMLLoopSpec) {
            try {
                TextualJMLLoopSpec textualLoopSpec = (TextualJMLLoopSpec) c;
                result = jsf.createJMLLoopInvariant(pm, loop, textualLoopSpec);
            } catch (SLWarningException e) {
                warnings = warnings.add(e.getWarning());
            }
        }

        return result;
    }

    
    @Override    
    public ImmutableSet<PositionedString> getWarnings() {
        return JMLTransformer.getWarningsOfLastInstance().union(warnings);
    }
}