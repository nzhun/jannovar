package de.charite.compbio.jannovar.filter;

import htsjdk.variant.variantcontext.VariantContext;

import java.util.HashSet;

/**
 * Check that the VCF file is sorted by coordinate.
 *
 * Since VCF files do not have to provide a reference name dictionary in their header, validating the sort order of the
 * chromosomes is tricky. Instead, we check that there is no run of VCF records for a chromosome, something on a
 * different chromsome, and then a previous one again. Within one chromosome, we check by change begin position.
 *
 * In case of problems, {@link #put} throws a {@link FilterException}.
 *
 * @author Manuel Holtgrewe <manuel.holtgrewe@charite.de>
 */
public class CoordinateSortChecker implements VariantContextFilter {

	/** next filter in pipeline */
	VariantContextFilter nextFilter;

	/** set of seen chromosome names */
	final private HashSet<String> seenChromosomes = new HashSet<String>();

	/** previously seen variant context */
	private VariantContext prevVC = null;

	/** Initialize the checker */
	public CoordinateSortChecker(VariantContextFilter filter) {
		this.nextFilter = filter;
	}

	@Override
	public void put(FlaggedVariant fv) throws FilterException {
		VariantContext vc = fv.vc;

		// perform sortedness check and throw exception otherwise
		if (prevVC != null)
			if (!vc.getChr().equals(prevVC.getChr())) { // change in chromosomes
				if (seenChromosomes.contains(vc.getChr()))
					throw new FilterException("Unsorted VCF file, seen " + vc.getChr() + " twice!");
			} else {
				if (vc.getStart() < prevVC.getStart())
					throw new FilterException("Unsorted VCF file, seen " + vc.getStart() + " < " + prevVC.getStart());
			}

		// pass through to next filter
		nextFilter.put(fv);

		// update prevVC reference
		prevVC = vc;
	}

	@Override
	public void finish() throws FilterException {
		nextFilter.finish();
	}

}
