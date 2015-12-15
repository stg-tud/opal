/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
// Author: Thorsten Jacobi

package org.opalj.bdl.ui.outline

import org.eclipse.xtext.ui.editor.outline.impl.DocumentRootNode
import org.eclipse.emf.ecore.EObject
import org.opalj.bdl.bDL.BDLPackage
import org.eclipse.xtext.ui.editor.outline.IOutlineNode
import org.eclipse.xtext.ui.editor.outline.impl.EStructuralFeatureNode
import org.opalj.bdl.bDL.IssueElement
import java.util.HashSet
import org.opalj.bdl.services.BDLGrammarAccess
import com.google.inject.Inject
import java.util.HashMap
import org.opalj.bdl.bDL.IssueCategoryElement
import org.opalj.bdl.bDL.IssueCategories

/**
 * Customization of the default outline structure.
 *
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#outline
 */
class BDLOutlineTreeProvider extends org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider {
	
	// to add/remove filters edit the following things:
	//	1) add/remove the static key
	//	2) add/remove to/from KEYS_FILTERBY
	//	3) add/remove in showPossibleOptionsForFilter
	//	4) add/remove in getFilteredIssues
	
	public static String KEY_FILTERBY_TYPE 			= 'filter by type';
	public static String KEY_FILTERBY_RELEVANCE		= 'filter by relevance';
	public static String KEY_FILTERBY_CATEGORY		= 'filter by category';
	public static String KEY_FILTERBY_KINDS			= 'filter by kind';
	public static String KEY_FILTERBY_PACKAGES		= 'filter by package';
	public static String KEY_FILTERBY_CLASS			= 'filter by class';
	
	public static String[] KEYS_FILTERBY = #[KEY_FILTERBY_TYPE, KEY_FILTERBY_RELEVANCE, KEY_FILTERBY_CATEGORY, KEY_FILTERBY_KINDS, KEY_FILTERBY_PACKAGES, KEY_FILTERBY_CLASS]
	
	@Inject package extension BDLGrammarAccess
	
	// generates the root structure
	override protected _createChildren(DocumentRootNode parentNode, EObject modelElement) {
		// show the default document structure
		createNode(parentNode, modelElement);
		// show filters
		showPossibleFilters(parentNode, modelElement, new HashSet(KEYS_FILTERBY));
	}
	
	override createChildren(IOutlineNode parent, EObject modelElement) {
		
		if (!(parent instanceof EStructuralFeatureNode)){
			super.createChildren(parent, modelElement);
			return;
		}
		
		var appliedFilters = getAppliedFilters(parent);
		var HashSet<IssueElement> filteredIssues = getFilteredIssues(modelElement, appliedFilters);

		if (KEYS_FILTERBY.contains(parent.text)){ // a filter is selected, show possible parameters
			showPossibleOptionsForFilter(parent, modelElement, parent.text as String, filteredIssues);
		}else{ // some filter was selected, show entries
			var HashSet<String> possible = getPossibleFilters(appliedFilters);
			showPossibleFilters(parent, modelElement, possible);
			
			// show filtered elements
			for (IssueElement issue : filteredIssues)
				createNode(parent , issue);
		}

	}
	
	def getPossibleFilters(HashMap<String,String> used){
		var possible = new HashSet<String>(KEYS_FILTERBY);
		
		for (String sKey : used.keySet)
			possible.remove(sKey);
		
		return possible;
	}
	def getAppliedFilters(IOutlineNode parent){
		var ret = new HashMap<String,String>();
		
		var IOutlineNode previous = null;
		var current = parent;
		while (current != null){
			if ((current instanceof EStructuralFeatureNode) && (previous != null)){
				if (KEYS_FILTERBY.contains(current.text))
					ret.put(current.text as String, previous.text as String);
			}
			previous = current;
			current = current.parent;
		}
		
		return ret;
	}
	
	def showPossibleFilters(IOutlineNode parent, EObject modelElement, HashSet<String> possible){
		for (String sFilter : possible){
			createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, sFilter, false);
		}
	}
	
	def showPossibleOptionsForFilter(IOutlineNode parent, EObject modelElement, String filter, HashSet<IssueElement> currentIssues){
		var created = new HashSet();

		for (IssueElement issue: currentIssues)
			if (filter.equals(KEY_FILTERBY_TYPE)){
				for (var i = 0; i < issue.name.length; i++)
					if (!created.contains(issue.name.get(i))){
						created.add(issue.name.get(i));
						createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, issue.name.get(i), false);	
					}
			}else if (filter.equals(KEY_FILTERBY_RELEVANCE)){
				if (!created.contains(issue.relevance.relevance)){
					created.add(issue.relevance.relevance);
					createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, issue.relevance.relevance.toString, false);
				}
			}else if (filter.equals(KEY_FILTERBY_CATEGORY)){
				for (IssueCategories cat : issue.categories.elements){
					var String sCat = getIssueCategoryKey(cat);
					if (!created.contains(sCat)){
						created.add(sCat);
						createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, sCat, false);
					}	
				}
			}else if (filter.equals(KEY_FILTERBY_KINDS)){
				for (String kind: issue.kinds.elements){
					if (!created.contains(kind)){
						created.add(kind);
						createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, kind, false);
					}	
				}
			}else if (filter.equals(KEY_FILTERBY_PACKAGES)){
				if (!created.contains(issue.package.package)){
					created.add(issue.package.package);
					createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, issue.package.package.toString, false);
				}
			}else if (filter.equals(KEY_FILTERBY_CLASS)){
				if (!created.contains(issue.class_.class_)){
					created.add(issue.class_.class_);
					createEStructuralFeatureNode(parent, modelElement, BDLPackage.Literals.MODEL_CONTAINER__NAME, null, issue.class_.class_.toString, false);
				}
			}
	}
	
	
	def getFilteredIssues(EObject modelElement, HashMap<String,String> appliedFilters){
		var ret = new HashSet<IssueElement>();
		
		var iter = modelElement.eAllContents;
		while (iter.hasNext){
			var current = iter.next;
			if (current.eClass.name.equals('IssueElement')){
				var issue = current as IssueElement;
				
				var boolean isOK = true;
				for (String filter: appliedFilters.keySet){
					var boolean thisFilter = false;
					if (filter.equals(KEY_FILTERBY_TYPE)){
						for (var i = 0; i < issue.name.length; i++){
							if (issue.name.get(i).equals(appliedFilters.get(filter)))
								thisFilter = true;
						}
					}else if (filter.equals(KEY_FILTERBY_RELEVANCE)){
						if (issue.relevance.relevance.toString.equals(appliedFilters.get(filter)))
							thisFilter = true;
					}else if (filter.equals(KEY_FILTERBY_CATEGORY)){
						for (var i = 0; i < issue.categories.elements.length; i++){
							var catK = getIssueCategoryKey(issue.categories.elements.get(i));
							if (catK.equals(appliedFilters.get(filter)))
								thisFilter = true;
						}
					}else if (filter.equals(KEY_FILTERBY_KINDS)){
						for (var i = 0; i < issue.kinds.elements.length; i++){
							if (issue.kinds.elements.get(i).equals(appliedFilters.get(filter)))
								thisFilter = true;
						}
					}else if (filter.equals(KEY_FILTERBY_PACKAGES)){
						if (issue.package.package.toString.equals(appliedFilters.get(filter)))
							thisFilter = true;
					}else if (filter.equals(KEY_FILTERBY_CLASS)){
						if (issue.class_.class_.toString.equals(appliedFilters.get(filter)))
							thisFilter = true;
					}

					isOK = isOK && thisFilter;
				}
			
				if (isOK)
					ret.add(issue);
			}
		}
		
		return ret;
	}
	
	
	// override for the category element ... troublemaker!
	override protected _isLeaf(EObject modelElement) {
		if (modelElement instanceof IssueCategoryElement) return true;
		super._isLeaf(modelElement)
	}
	


	// map the categories to specific keys to ensure they can compared
	// also apparently that auto generated IssueCategories-Class does not offer
	// an obvious function to get the value
	public static String KEY_CATEGORY_BUG 				= "bug";
	public static String KEY_CATEGORY_COMPREHENSIBILITY = "comprehensibility";
	public static String KEY_CATEGORY_SMELL 			= "smell";
	public static String KEY_CATEGORY_PERFORMANCE 		= "performance";
	def getIssueCategoryKey(IssueCategories cat){
		if ((cat.bug != null) && (cat.bug.length > 0))
			return KEY_CATEGORY_BUG;
		if ((cat.smell != null) && (cat.smell.length > 0))
			return KEY_CATEGORY_SMELL;
		if ((cat.comprehensibility != null) && (cat.comprehensibility.length > 0))
			return KEY_CATEGORY_COMPREHENSIBILITY;
		if ((cat.performance != null) && (cat.performance.length > 0))
			return KEY_CATEGORY_PERFORMANCE;	
	}
}
