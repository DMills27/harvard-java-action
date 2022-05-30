package com.realdecoy.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class TaxonomyItem {

	private String generatedUid;

	@SerializedName("Name")
	private String name;

	@SerializedName("RelatedTerms")
	private ArrayList<TaxonomyItem> relatedTerms;

	@SerializedName("VocabName")
	private String taxonomyName;
	
	@SerializedName("Uid")
	private String uid;
	
	public boolean hasRelatedTerms() {
		return (this.relatedTerms != null && this.relatedTerms.size() > 0);
	}

	public String getGeneratedUid() {
		return generatedUid;
	}

	public void setGeneratedUid(String generatedUid) {
		this.generatedUid = generatedUid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<TaxonomyItem> getRelatedTerms() {
		return relatedTerms;
	}

	public void setRelatedTerms(ArrayList<TaxonomyItem> relatedTerms) {
		this.relatedTerms = relatedTerms;
	}

	public String getTaxonomyName() {
		return taxonomyName;
	}

	public void setTaxonomyName(String taxonomyName) {
		this.taxonomyName = taxonomyName;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}
	
}
