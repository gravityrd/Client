package com.gravityrd.recengclient.webshop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gravityrd.receng.web.webshop.jsondto.GravityNameValue;
import com.gravityrd.receng.web.webshop.jsondto.GravityRecommendationContext;
import com.gravityrd.receng.web.webshop.jsondto.facet.FacetRequest;
import com.gravityrd.receng.web.webshop.jsondto.facet.FacetRequest.Filter;
import com.gravityrd.receng.web.webshop.jsondto.facet.FacetRequest.FilterLogic;
import com.gravityrd.receng.web.webshop.jsondto.facet.Range;
import com.gravityrd.receng.web.webshop.jsondto.facet.RangeFacetRequest;
import com.gravityrd.receng.web.webshop.jsondto.facet.TermFacetRequest;
import com.gravityrd.receng.web.webshop.jsondto.facet.TermFacetRequest.Order;
import com.gravityrd.recengclient.webshop.GravityRecommendationContextBuilder.FacetBuilder.FacetRequestBuilder;

public final class GravityRecommendationContextBuilder {

	private int recommendationTime = (int)(System.currentTimeMillis() / 1000);
	private int numberLimit;
	private String scenarioId;
	private List<GravityNameValue> nameValues = new ArrayList<GravityNameValue>();
	private Set<String> resultNameValues = new HashSet<String>();
	private HashMap<String, String[]> resultNameValueFilters = new HashMap<String, String[]>();
	private List<FacetRequest<?>> facets = new ArrayList<FacetRequest<?>>();
	

	public static final class FacetBuilder {

		public static abstract class FacetRequestBuilder<B extends FacetRequestBuilder<B, T, F>, T extends FacetRequest<F>, F> {
			protected String field;
			protected FilterLogic filterLogic;
			protected List<F> filterValues = new ArrayList<F>();
			
			abstract T build();
			
			public FacetRequestBuilder(String field) {
				this.field = field;
			}

			@SuppressWarnings("unchecked")
			public B filterLogic(FilterLogic filterLogic) {
				this.filterLogic = filterLogic;
				return (B) this;
			}
			
			@SuppressWarnings("unchecked")
			public B filterValue(F value) {
				filterValues.add(value);
				return (B) this;
			}

			@SuppressWarnings("unchecked")
			public B filterValues(Collection<F> values) {
				filterValues.addAll(values);
				return (B) this;
			}
		}
		
		public static final class TermFacetBuilder extends FacetRequestBuilder<TermFacetBuilder, TermFacetRequest, String> {
			private int count;
			private Order order;

			public TermFacetBuilder(String field, int count) {
				super(field);
				this.count = count;
			}

			public TermFacetBuilder order(Order order) {
				this.order = order;
				return this;
			}
			
			
			@Override
			public TermFacetRequest build() {
				Filter<String> filter = null;
				if (filterValues.size() > 0) {
					filter = new Filter<String>(filterLogic, filterValues);
				}
				return new TermFacetRequest(field, count, order, filter);
			}
		}

		public static final class RangeFacetBuilder extends FacetRequestBuilder<RangeFacetBuilder, RangeFacetRequest, Range>{
			private List<Range> ranges = new ArrayList<Range>();

			public RangeFacetBuilder(String field) {
				super(field);
			}

			public RangeFacetBuilder(String field, Collection<Range> ranges) {
				super(field);
				this.ranges.addAll(ranges);
			}

			public RangeFacetBuilder addRange(Range range) {
				this.ranges.add(range);
				return this;
			}

			public RangeFacetBuilder addRanges(Collection<Range> ranges) {
				this.ranges.addAll(ranges);
				return this;
			}
			
			public RangeFacetBuilder addRange(Double from, Double to) {
				this.ranges.add(new Range(from, to));
				return this;
			}

			public RangeFacetBuilder filterValue(Double from, Double to) {
				filterValues.add(new Range(from, to));
				return this;
			}
			
			@Override
			public RangeFacetRequest build() {
				if (ranges.size() == 0) throw new IllegalStateException("At least one range should be specified");
				Filter<Range> filter = null;
				if (filterValues.size() > 0) {
					filter = new Filter<Range>(filterLogic, filterValues);
				}
				return new RangeFacetRequest(field, ranges, filter);
			}
			
		}
		
		public static TermFacetBuilder term(String field, int count) {
			return new TermFacetBuilder(field, count);
		}

		public static RangeFacetBuilder range(String field) {
			return new RangeFacetBuilder(field);
		}

		public static RangeFacetBuilder range(String field, List<Range> ranges) {
			return new RangeFacetBuilder(field, ranges);
		}
	}

	
	public GravityRecommendationContextBuilder(String scenarioId, int numberLimit) {
		this.numberLimit = numberLimit;
		this.scenarioId = scenarioId;
	}

	public GravityRecommendationContextBuilder setRecommendationTime(int recommendationTime) {
		this.recommendationTime = recommendationTime;
		return this;
	}

	public GravityRecommendationContextBuilder addNameValue(String name, String value) {
		this.nameValues.add(new GravityNameValue(name, value));
		return this;
	}

	public GravityRecommendationContextBuilder addNameValue(List<String> names, List<String> values) {
		if (names.size() != values.size()) {
			throw new IllegalArgumentException("Size of names and values should be equal!");
		}
		for (int i=0; i < names.size(); i++) {
			this.nameValues.add(new GravityNameValue(names.get(i), values.get(i)));
		}
		return this;
	}

	public GravityRecommendationContextBuilder addNameValue(Collection<GravityNameValue> nameValues) {
		this.nameValues.addAll(nameValues);
		return this;
	}

	public GravityRecommendationContextBuilder addResultNameValue(String name) {
		this.resultNameValues.add(name);
		return this;
	}
	
	public GravityRecommendationContextBuilder addResultNameValues(Collection<String> names) {
		this.resultNameValues.addAll(names);
		return this;
	}
	
	public GravityRecommendationContextBuilder addResultNameValueFilter(String name, Collection<String> values) {
		this.resultNameValueFilters.put(name, values.toArray(new String[values.size()]));
		return this;
	}

	public GravityRecommendationContextBuilder addFacet(FacetRequest<?> facet) {
		this.facets.add(facet);
		return this;
	}
		
	public GravityRecommendationContextBuilder addFacet(FacetRequestBuilder<?,?,?> facetBuilder) {
		this.facets.add(facetBuilder.build());
		return this;
	}

	public GravityRecommendationContext build() {
		GravityRecommendationContext context = new GravityRecommendationContext();
		context.scenarioId = scenarioId;
		context.numberLimit = numberLimit;
		context.recommendationTime = recommendationTime;
		context.nameValues = nameValues.toArray(new GravityNameValue[nameValues.size()]);
		context.resultNameValues = resultNameValues.toArray(new String[resultNameValues.size()]);
		context.resultNameValueFilters = resultNameValueFilters.size() > 0 ? resultNameValueFilters : null;
		context.facets = facets.size() > 0 ? facets : null;
		return context;
	}
}
