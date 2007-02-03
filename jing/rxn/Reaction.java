//!********************************************************************************
//!
//!********************************************************************************
//!
//!    RMG: Reaction Mechanism Generator
//!
//!    Copyright: Jing Song, MIT, 2002, all rights reserved
//!
//!    Author's Contact: jingsong@mit.edu
//!
//!    Restrictions:
//!    (1) RMG is only for non-commercial distribution; commercial usage
//!        must require other written permission.
//!    (2) Redistributions of RMG must retain the above copyright
//!        notice, this list of conditions and the following disclaimer.
//!    (3) The end-user documentation included with the redistribution,
//!        if any, must include the following acknowledgment:
//!        "This product includes software RMG developed by Jing Song, MIT."
//!        Alternately, this acknowledgment may appear in the software itself,
//!        if and wherever such third-party acknowledgments normally appear.
//!
//!    RMG IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
//!    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
//!    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//!    DISCLAIMED.  IN NO EVENT SHALL JING SONG BE LIABLE FOR
//!    ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
//!    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
//!    OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
//!    OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
//!    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//!    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
//!    THE USE OF RMG, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//!
//!******************************************************************************



package jing.rxn;


import java.io.*;
import jing.chem.*;
import java.util.*;
import jing.param.*;
import jing.mathTool.*;
import jing.chemParser.*;
import jing.chem.Species;
import jing.param.Temperature;
import jing.rxnSys.SystemSnapshot;

//## package jing::rxn

//----------------------------------------------------------------------------
//jing\rxn\Reaction.java
//----------------------------------------------------------------------------

/**
Immutable objects.
*/
//## class Reaction
public class Reaction {

  protected static double BIMOLECULAR_RATE_UPPER = 1.0E100;		//## attribute BIMOLECULAR_RATE_UPPER

  protected static double UNIMOLECULAR_RATE_UPPER = 1.0E100;		//## attribute UNIMOLECULAR_RATE_UPPER

  protected String comments = "";		//## attribute comments

  protected Kinetics fittedReverseKinetics = null;		//## attribute fittedReverseKinetics

  protected double rateConstant =0 ; 
  
  protected Reaction reverseReaction = null;		//## attribute reverseReaction

  protected Kinetics kinetics;
  protected Structure structure;
  protected double UpperBoundRate=0;//svp
  protected double LowerBoundRate = 0;//svp
	protected Kinetics additionalKinetics = null;  //This is incase a reaction has two completely different transition states.
	protected boolean finalized = false;
	protected String ChemkinString = null;
  // Constructors

  //## operation Reaction()
  public  Reaction() {
      //#[ operation Reaction()
      //#]
  }
  //## operation Reaction(Structure,RateConstant)
  private  Reaction(Structure p_structure, Kinetics p_kinetics) {
      //#[ operation Reaction(Structure,RateConstant)
      structure = p_structure;
      kinetics = p_kinetics;
	  //rateConstant = calculateTotalRate(Global.temperature);
      //#]
  }

  //## operation allProductsIncluded(HashSet)
  public boolean allProductsIncluded(HashSet p_speciesSet) {
      //#[ operation allProductsIncluded(HashSet)
      Iterator iter = getProducts();
      while (iter.hasNext()) {
      	Species spe = ((Species)iter.next());
      	if (!p_speciesSet.contains(spe)) return false;
      }
      return true;
      //#]
  }

  //## operation allReactantsIncluded(HashSet)
  public boolean allReactantsIncluded(HashSet p_speciesSet) {
      //#[ operation allReactantsIncluded(HashSet)
      if (p_speciesSet == null) throw new NullPointerException();
      Iterator iter = getReactants();
      while (iter.hasNext()) {
      	Species spe = ((Species)iter.next());
      	if (!p_speciesSet.contains(spe)) return false;
      }
      return true;
      //#]
  }

  /**
  Calculate this reaction's thermo parameter.  Basically, make addition of the thermo parameters of all the reactants and products.
  */
  //## operation calculateHrxn(Temperature)
  public double calculateHrxn(Temperature p_temperature) {
      //#[ operation calculateHrxn(Temperature)
      return structure.calculateHrxn(p_temperature);
      //#]
  }

  //## operation calculateKeq(Temperature)
  public double calculateKeq(Temperature p_temperature) {
      //#[ operation calculateKeq(Temperature)
      return structure.calculateKeq(p_temperature);
      //#]
  }

  //## operation calculateKeqUpperBound(Temperature)
  //svp
    public double calculateKeqUpperBound(Temperature p_temperature) {
      //#[ operation calculateKeqUpperBound(Temperature)
      return structure.calculateKeqUpperBound(p_temperature);
      //#]
    }

  //## operation calculateKeqLowerBound(Temperature)
  //svp
    public double calculateKeqLowerBound(Temperature p_temperature) {
      //#[ operation calculateKeqLowerBound(Temperature)
      return structure.calculateKeqLowerBound(p_temperature);
      //#]
    }

  
  public double calculateTotalRate(Temperature p_temperature){
  	double rate =0;
	Temperature stdtemp = new Temperature(298,"K");
	double Hrxn = calculateHrxn(stdtemp);
  	if (isForward()){
  		Iterator kineticIter = getAllKinetics().iterator();
      	while (kineticIter.hasNext()){
      		Kinetics k = (Kinetics)kineticIter.next();
			if (k instanceof ArrheniusEPKinetics)
				rate = rate + k.calculateRate(p_temperature,Hrxn);
			else 
				rate = rate + k.calculateRate(p_temperature);
      	}
		
      	return rate;
  	}
  	else if (isBackward()){
  		Reaction r = getReverseReaction();
  		rate = r.calculateTotalRate(p_temperature);
  		return rate*calculateKeq(p_temperature);
  	}
  	else {
  		throw new InvalidReactionDirectionException();
  	}
  	
  }

  //## operation calculateUpperBoundRate(Temperature)
  //svp
    public double calculateUpperBoundRate(Temperature p_temperature){
      //#[ operation calculateUpperBoundRate(Temperature)
      if (isForward()){
        double A;
        double E;
        double n;
        
        A = kinetics.getA().getUpperBound();
        E = kinetics.getE().getLowerBound();      
        n = kinetics.getN().getUpperBound();
              
        if (A > 1E300) {
          A = kinetics.getA().getValue()*1.2;
		  }
        //Kinetics kinetics = getRateConstant().getKinetics();
        if (kinetics instanceof ArrheniusEPKinetics){
          ArrheniusEPKinetics arrhenius = (ArrheniusEPKinetics)kinetics;
          double H = calculateHrxn(p_temperature);
          if (H < 0) {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha*H;
            }
          }
          else {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha*H;
            }
          }
          }
          if (E < 0){
            E = 0;
          }
        double k = A*Math.pow(p_temperature.getK(),n)*Math.exp(-E/GasConstant.getKcalMolK()/p_temperature.getK());
        k *= getStructure().getRedundancy();
        UpperBoundRate = k;
        return k;
      }
      else if (isBackward()) {
        Reaction r = getReverseReaction();
        if (r == null) throw new NullPointerException("Reverse reaction is null.\n" + structure.toString());
        if (!r.isForward()) throw new InvalidReactionDirectionException();
        double A = kinetics.getA().getUpperBound();
        double E = kinetics.getE().getLowerBound();
        double n = kinetics.getN().getUpperBound();
        if (A > 1E300) {
          A = kinetics.getA().getValue()*1.2;
       }
        //Kinetics kinetics = getRateConstant().getKinetics();
        if (kinetics instanceof ArrheniusEPKinetics){
          ArrheniusEPKinetics arrhenius = (ArrheniusEPKinetics)kinetics;
          double H = calculateHrxn(p_temperature);
          if (H < 0) {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha*H;
            }
          }
          else {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha*H;
            }
          }
        }
        if (E < 0){
          E = 0;
        }
        double k = A*Math.pow(p_temperature.getK(),n)*Math.exp(-E/GasConstant.getKcalMolK()/p_temperature.getK());
        k *= getStructure().getRedundancy();
        UpperBoundRate = k*calculateKeqUpperBound(p_temperature);
        return UpperBoundRate;
      }
      else{
        throw new InvalidReactionDirectionException();
      }
      //#]
    }

  //## operation calculateLowerBoundRate(Temperature)
  //svp
    public double calculateLowerBoundRate(Temperature p_temperature){
      //#[ operation calculateLowerBoundRate(Temperature)
      if (isForward()){
        double A = kinetics.getA().getLowerBound();
        double E = kinetics.getE().getUpperBound();
        double n = kinetics.getN().getLowerBound();
        if (A > 1E300 || A <= 0) {
          A = kinetics.getA().getValue()/1.2;
       }
        //Kinetics kinetics = getRateConstant().getKinetics();
        if (kinetics instanceof ArrheniusEPKinetics){
          ArrheniusEPKinetics arrhenius = (ArrheniusEPKinetics)kinetics;
          double H = calculateHrxn(p_temperature);
          if (H < 0) {
            if (arrhenius.getAlpha().getValue()>0){
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha*H;
            }
          }
          else {
            if (arrhenius.getAlpha().getValue()>0){
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha*H;
            }
          }
        }

        double k = A*Math.pow(p_temperature.getK(),n)*Math.exp(-E/GasConstant.getKcalMolK()/p_temperature.getK());
        k *= getStructure().getRedundancy();
        LowerBoundRate = k;
        return k;
      }
      else if (isBackward()) {
        Reaction r = getReverseReaction();
        if (r == null) throw new NullPointerException("Reverse reaction is null.\n" + structure.toString());
        if (!r.isForward()) throw new InvalidReactionDirectionException();
        double A = kinetics.getA().getLowerBound();
        double E = kinetics.getE().getUpperBound();
        double n = kinetics.getN().getLowerBound();
        if (A > 1E300) {
           A = kinetics.getA().getValue()/1.2;
        }
        if (kinetics instanceof ArrheniusEPKinetics){
          ArrheniusEPKinetics arrhenius = (ArrheniusEPKinetics)kinetics;
          double H = calculateHrxn(p_temperature);
          if (H < 0) {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha*H;
            }
          }
          else {
            if (arrhenius.getAlpha().getValue() > 0){
              double alpha = arrhenius.getAlpha().getUpperBound();
              E = E + alpha * H;
            }
            else{
              double alpha = arrhenius.getAlpha().getLowerBound();
              E = E + alpha*H;
            }
          }
        }

        double k = A*Math.pow(p_temperature.getK(),n)*Math.exp(-E/GasConstant.getKcalMolK()/p_temperature.getK());
        k *= getStructure().getRedundancy();
        LowerBoundRate = k*calculateKeqLowerBound(p_temperature);
        return LowerBoundRate;
      }

      else{
        throw new InvalidReactionDirectionException();
      }
      //#]
    }


  //## operation calculateSrxn(Temperature)
  public double calculateSrxn(Temperature p_temperature) {
      //#[ operation calculateSrxn(Temperature)
      return structure.calculateSrxn(p_temperature);
      //#]
  }

  //## operation calculateThirdBodyCoefficient(SystemSnapshot)
  public double calculateThirdBodyCoefficient(SystemSnapshot p_presentStatus) {
      //#[ operation calculateThirdBodyCoefficient(SystemSnapshot)
      if (!(this instanceof ThirdBodyReaction)) return 1;
      else {
      	return ((ThirdBodyReaction)this).calculateThirdBodyCoefficient(p_presentStatus);
      }
      //#]
  }

  //## operation checkRateRange()
  public boolean checkRateRange() {
      //#[ operation checkRateRange()
      Temperature t = new Temperature(1500,"K");

      double rate = calculateTotalRate(t);

      if (getReactantNumber() == 2) {
      	if (rate > BIMOLECULAR_RATE_UPPER) return false;
      }
      else if (getReactantNumber() == 1) {
      	if (rate > UNIMOLECULAR_RATE_UPPER) return false;
      }
      else throw new InvalidReactantNumberException();

      return true;
      //#]
  }

  //## operation contains(Species)
  public boolean contains(Species p_species) {
      //#[ operation contains(Species)
      if (containsAsReactant(p_species) || containsAsProduct(p_species)) return true;
      else return false;


      //#]
  }

  //## operation containsAsProduct(Species)
  public boolean containsAsProduct(Species p_species) {
      //#[ operation containsAsProduct(Species)
      Iterator iter = getProducts();
      while (iter.hasNext()) {
      	//ChemGraph cg = (ChemGraph)iter.next();
      	Species spe = (Species)iter.next();
      	if (spe.equals(p_species)) return true;
      }

      return false;
      //#]
  }

  //## operation containsAsReactant(Species)
  public boolean containsAsReactant(Species p_species) {
      //#[ operation containsAsReactant(Species)
      Iterator iter = getReactants();
      while (iter.hasNext()) {
      	//ChemGraph cg = (ChemGraph)iter.next();
      	Species spe = (Species)iter.next();
      	if (spe.equals(p_species)) return true;
      }

      return false;
      //#]
  }

  //## operation equals(Object)
  public boolean equals(Object p_reaction) {
      //#[ operation equals(Object)
      if (this == p_reaction) return true;

      if (!(p_reaction instanceof Reaction)) return false;

      Reaction r = (Reaction)p_reaction;

      if (!getStructure().equals(r.getStructure())) return false;

      return true;


      //#]
  }

  //## operation fitReverseKineticsPrecisely()
  public void fitReverseKineticsPrecisely() {
      //#[ operation fitReverseKineticsPrecisely()
      if (isForward()) {
      	fittedReverseKinetics = null;
      }
      else {

      	String result = "";
      	for (double t = 300.0; t<1500.0; t+=50.0) {
      		double rate = calculateTotalRate(new Temperature(t,"K"));
      		result += String.valueOf(t) + '\t' + String.valueOf(rate) + '\n';
      	}

          // run fit3p
      	String dir = System.getProperty("RMG.workingDirectory");

      	File fit3p_input;

      	try {
      	        // prepare fit3p input file, "input.dat" is the input file name
      	        fit3p_input = new File("fit3p/input.dat");
      	        FileWriter fw = new FileWriter(fit3p_input);
      	        fw.write(result);
      	        fw.close();
      	}
      	catch (IOException e) {
      	        System.out.println("Wrong input file for fit3p!");
      	        System.out.println(e.getMessage());
      	        System.exit(0);
      	}

      	try {
      	    // system call for fit3p
      		String[] command = {"fit3p/fit3pbnd.exe"};
      		File runningDir = new File(dir + "software/fit3p");
      	    Process fit = Runtime.getRuntime().exec(command, null, runningDir);
      	    int exitValue = fit.waitFor();
      	}
      	catch (Exception e) {
      	    System.out.println("Error in run fit3p!");
      	    System.out.println(e.getMessage());
      	    System.exit(0);
      	}

      	// parse the output file from chemdis
      	try {
      	        String fit3p_output = "fit3p/output.dat";

      	        FileReader in = new FileReader(fit3p_output);
      	        BufferedReader data = new BufferedReader(in);

      	        String line = ChemParser.readMeaningfulLine(data);
      	        line = line.trim();
      	    	StringTokenizer st = new StringTokenizer(line);
      	        String A = st.nextToken();
      	        String temp = st.nextToken();
      	        temp = st.nextToken();
      	        temp = st.nextToken();
      	        double Ar = Double.parseDouble(temp);

      			line = ChemParser.readMeaningfulLine(data);
      	        line = line.trim();
      	        st = new StringTokenizer(line);
      	        String n = st.nextToken();
      	        temp = st.nextToken();
      	        temp = st.nextToken();
      	        double nr = Double.parseDouble(temp);

      			line = ChemParser.readMeaningfulLine(data);
      	        line = line.trim();
      	        st = new StringTokenizer(line);
      	        String E = st.nextToken();
      	        temp = st.nextToken();
      	        temp = st.nextToken();
      	        temp = st.nextToken();
      	        double Er = Double.parseDouble(temp);
      			if (Er < 0) {
      				System.err.println(getStructure().toString());
      				System.err.println("fitted Er < 0: "+Double.toString(Er));
      				double increase = Math.exp(-Er/GasConstant.getKcalMolK()/715.0);
      				double deltan = Math.log(increase)/Math.log(715.0);
      				System.err.println("n enlarged by factor of: " + Double.toString(deltan));
      				nr += deltan;
      				Er = 0;

      			}


      	    	UncertainDouble udAr = new UncertainDouble(Ar, 0, "Adder");
      	        UncertainDouble udnr = new UncertainDouble(nr, 0, "Adder");
      	        UncertainDouble udEr = new UncertainDouble(Er, 0, "Adder");

      	        fittedReverseKinetics = new ArrheniusKinetics(udAr, udnr , udEr, "300-1500", 1, "fitting from forward and thermal",null);
      	        in.close();
      	}
      	catch (Exception e) {
      	        System.out.println("Error in read output.dat from fit3p!");
      	        System.out.println(e.getMessage());
      	        System.exit(0);
      	}
      }

      return;
      //#]
  }

  //## operation fitReverseKineticsRoughly()
  public void fitReverseKineticsRoughly() {
      //#[ operation fitReverseKineticsRoughly()
      // now is a rough fitting
      if (isForward()) {
      	fittedReverseKinetics = null;
      }
      else {
      	double temp = 715;
      	Kinetics k = getKinetics();
      	double doubleAlpha;
      	if (k instanceof ArrheniusEPKinetics) doubleAlpha = ((ArrheniusEPKinetics)k).getAlphaValue();
      	else doubleAlpha = 0;

      	double Hrxn = calculateHrxn(new Temperature(temp,"K"));
      	double Srxn = calculateSrxn(new Temperature(temp, "K"));
      	double doubleEr = k.getEValue() - (doubleAlpha-1)*Hrxn;
      	if (doubleEr < 0) {
      		System.err.println("fitted Er < 0: "+Double.toString(doubleEr));
      		System.err.println(getStructure().toString());
      		doubleEr = 0;
      	}

      	UncertainDouble Er = new UncertainDouble(doubleEr, k.getE().getUncertainty(), k.getE().getType());
      	UncertainDouble n = new UncertainDouble(0,0, "Adder");

      	double doubleA = k.getAValue()* Math.pow(temp, k.getNValue())* Math.exp(Srxn/GasConstant.getCalMolK());
      	doubleA *= Math.pow(GasConstant.getCCAtmMolK()*temp, -getStructure().getDeltaN());
      	fittedReverseKinetics = new ArrheniusKinetics(new UncertainDouble(doubleA, 0, "Adder"), n , Er, "300-1500", 1, "fitting from forward and thermal",null);


      }
      return;
      //#]
  }

  //## operation generateReverseReaction()
  public void generateReverseReaction() {
      //#[ operation generateReverseReaction()
      Structure s = getStructure();

      Kinetics k = getKinetics();
      Structure newS = s.generateReverseStructure();
	  newS.setRedundancy(s.getRedundancy());
      Reaction r = new Reaction(newS, k);

	  if (hasAdditionalKinetics()){
		  r.addAdditionalKinetics(additionalKinetics,1);
	  }
	  
      r.setReverseReaction(this);
      this.setReverseReaction(r);

      return;
      //#]
  }

  //## operation getComments()
  public String getComments() {
      //#[ operation getComments()
      return comments;
      //#]
  }

  //## operation getDirection()
  public int getDirection() {
      //#[ operation getDirection()
      return getStructure().getDirection();
      //#]
  }

  //## operation getFittedReverseKinetics()
  public Kinetics getFittedReverseKinetics() {
      //#[ operation getFittedReverseKinetics()
      if (fittedReverseKinetics == null) fitReverseKineticsPrecisely();
      return fittedReverseKinetics;
      //#]
  }

  /*//## operation getForwardRateConstant()
  public Kinetics getForwardRateConstant() {
      //#[ operation getForwardRateConstant()
      if (isForward()) return kinetics;
      else return null;
      //#]
  }
*/
  //## operation getKinetics()
  public Kinetics getKinetics() {
      //#[ operation getKinetics()
      if (isForward()) {
      	int red = structure.getRedundancy();
      	return kinetics.multiply(red);
      }
      else if (isBackward()) {
      	Reaction rr = getReverseReaction();
      	if (rr == null) throw new NullPointerException("Reverse reaction is null.\n" + structure.toString());
      	if (!rr.isForward()) throw new InvalidReactionDirectionException(structure.toString());
      	return rr.getKinetics();
      }
      else throw new InvalidReactionDirectionException(structure.toString());



      //#]
  }

  public void setKineticsComments(String p_string){
	  kinetics.setComments(p_string);
  }
  
  public void setKineticsSource(String p_string){
	  kinetics.setSource(p_string);
  }
  
  //## operation getUpperBoundRate(Temperature)
  public double getUpperBoundRate(Temperature p_temperature){//svp
    //#[ operation getUpperBoundRate(Temperature)
    if (UpperBoundRate == 0){
      calculateUpperBoundRate(p_temperature);
    }
    return UpperBoundRate;
    //#]
  }

  //## operation getLowerBoundRate(Temperature)
  public double getLowerBoundRate(Temperature p_temperature){//svp
    //#[ operation getLowerBoundRate(Temperature)
    if (LowerBoundRate == 0){
      calculateLowerBoundRate(p_temperature);
    }
    return LowerBoundRate;
    //#]
  }


  //## operation getProductList()
  public LinkedList getProductList() {
      //#[ operation getProductList()
      return structure.getProductList();
      //#]
  }

  public double getRateConstant(){
	  if (rateConstant == 0)
		  rateConstant = calculateTotalRate(Global.temperature);
	  return rateConstant;
  }
  
  //## operation getProductNumber()
  public int getProductNumber() {
      //#[ operation getProductNumber()
      return getStructure().getProductNumber();
      //#]
  }

  //## operation getProducts()
  public ListIterator getProducts() {
      //#[ operation getProducts()
      return structure.getProducts();
      //#]
  }

  /*//## operation getRateConstant()
  public Kinetics getRateConstant() {
      //#[ operation getRateConstant()
      if (isForward()) {
      	return rateConstant;
      }
      else if (isBackward()) {
      	Reaction rr = getReverseReaction();
      	if (rr == null) throw new NullPointerException("Reverse reaction is null.\n" + structure.toString());
      	if (!rr.isForward()) throw new InvalidReactionDirectionException(structure.toString());
      	return rr.getRateConstant();
      }
      else throw new InvalidReactionDirectionException(structure.toString());
      //#]
  }*/

  //## operation getReactantList()
  public LinkedList getReactantList() {
      //#[ operation getReactantList()
      return structure.getReactantList();
      //#]
  }

  //## operation getReactantNumber()
  public int getReactantNumber() {
      //#[ operation getReactantNumber()
      return getStructure().getReactantNumber();
      //#]
  }

  //## operation getReactants()
  public ListIterator getReactants() {
      //#[ operation getReactants()
      return structure.getReactants();
      //#]
  }

  //## operation getRedundancy()
  public int getRedundancy() {
      //#[ operation getRedundancy()
      return getStructure().getRedundancy();
      //#]
  }

	 public boolean hasResonanceIsomer() {
	        //#[ operation hasResonanceIsomer()
	        return (hasResonanceIsomerAsReactant() || hasResonanceIsomerAsProduct());
	        //#]
	    }

	    //## operation hasResonanceIsomerAsProduct()
	    public boolean hasResonanceIsomerAsProduct() {
	        //#[ operation hasResonanceIsomerAsProduct()
	        for (Iterator iter = getProducts(); iter.hasNext();) {
	        	Species spe = ((Species)iter.next());
	        	if (spe.hasResonanceIsomers()) return true;
	        }
	        return false;
	        //#]
	    }

	    //## operation hasResonanceIsomerAsReactant()
	    public boolean hasResonanceIsomerAsReactant() {
	        //#[ operation hasResonanceIsomerAsReactant()
	        for (Iterator iter = getReactants(); iter.hasNext();) {
				Species spe = ((Species)iter.next());
	        	if (spe.hasResonanceIsomers()) return true;
	        }
	        return false;
	        //#]
	    }

	    //## operation hasReverseReaction()
	    public boolean hasReverseReaction() {
	        //#[ operation hasReverseReaction()
	        return reverseReaction != null;
	        //#]
	    }


  //## operation hashCode()
  public int hashCode() {
      //#[ operation hashCode()
      // just use the structure's hashcode
      return structure.hashCode();


      //#]
  }

	   //## operation isDuplicated(Reaction)
  /*public boolean isDuplicated(Reaction p_reaction) {
      //#[ operation isDuplicated(Reaction)
      // the same structure, return true
      Structure str1 = getStructure();
      Structure str2 = p_reaction.getStructure();

      //if (str1.isDuplicate(str2)) return true;

      // if not the same structure, check the resonance isomers
      if (!hasResonanceIsomer()) return false;

      if (str1.equals(str2)) return true;
      else return false;


      //#]
  }*/

  //## operation isBackward()
  public boolean isBackward() {
      //#[ operation isBackward()
      return structure.isBackward();
      //#]
  }

  //## operation isForward()
  public boolean isForward() {
      //#[ operation isForward()
      return structure.isForward();
      //#]
  }

  //## operation isIncluded(HashSet)
  public boolean isIncluded(HashSet p_speciesSet) {
      //#[ operation isIncluded(HashSet)
      return (allReactantsIncluded(p_speciesSet) && allProductsIncluded(p_speciesSet));
      //#]
  }

  //## operation makeReaction(Structure,Kinetics,boolean)
  public static Reaction makeReaction(Structure p_structure, Kinetics p_kinetics, boolean p_generateReverse) {
      //#[ operation makeReaction(Structure,Kinetics,boolean)
      if (!p_structure.repOk()) throw new InvalidStructureException(p_structure.toChemkinString(false).toString());
      if (!p_kinetics.repOk()) throw new InvalidKineticsException(p_kinetics.toString());


      Reaction r = new Reaction(p_structure, p_kinetics);

      if (p_generateReverse) {
      	r.generateReverseReaction();
      }
      else {
      	r.setReverseReaction(null);
      }

      return r;
      //#]
  }

  //## operation reactantEqualsProduct()
  public boolean reactantEqualsProduct() {
      //#[ operation reactantEqualsProduct()
      return getStructure().reactantEqualsProduct();
      //#]
  }

  //## operation repOk()
  public boolean repOk() {
      //#[ operation repOk()
      if (!structure.repOk()) {
      	System.out.println("Invalid Reaction Structure:" + structure.toString());
      	return false;
      }

      if (!isForward() && !isBackward()) {
      	System.out.println("Invalid Reaction Direction: " + String.valueOf(getDirection()));
      	return false;
      }
      if (isBackward() && reverseReaction == null) {
      	System.out.println("Backward Reaction without a reversed reaction defined!");
      	return false;
      }

      /*if (!getRateConstant().repOk()) {
      	System.out.println("Invalid Rate Constant: " + getRateConstant().toString());
      	return false;
      }*/

      if (!getKinetics().repOk()) {
      	System.out.println("Invalid Kinetics: " + getKinetics().toString());
      	return false;
      }

      if (!checkRateRange()) {
      	System.out.println("reaction rate is higher than the upper rate limit!");
      	System.out.println(getStructure().toString());
      	Temperature tup = new Temperature(1500,"K");
      	if (isForward()) {
      		System.out.println("k(T=1500) = " + String.valueOf(calculateTotalRate(tup)));
      	}
      	else {
      		System.out.println("k(T=1500) = " + String.valueOf(calculateTotalRate(tup)));
      		System.out.println("Keq(T=1500) = " + String.valueOf(calculateKeq(tup)));
      		System.out.println("krev(T=1500) = " + String.valueOf(getReverseReaction().calculateTotalRate(tup)));
      	}
      	System.out.println(getKinetics());
      	return false;
      }
      return true;
      //#]
  }

  //## operation setReverseReaction(Reaction)
  public void setReverseReaction(Reaction p_reverseReaction) {
      //#[ operation setReverseReaction(Reaction)
      reverseReaction = p_reverseReaction;
      if (p_reverseReaction != null) reverseReaction.reverseReaction = this;
      //#]
  }


	  //## operation toChemkinString()
  public String toChemkinString(Temperature p_temperature) {
      //#[ operation toChemkinString()
	  if (ChemkinString != null)
		  return ChemkinString;
	  	StringBuilder result = new StringBuilder();
      	StringBuilder strucString = getStructure().toChemkinString(hasReverseReaction());
		Temperature stdtemp = new Temperature(298,"K");
		double Hrxn = calculateHrxn(stdtemp);
		if (hasAdditionalKinetics()){
			Iterator iter = getAllKinetics().iterator();
			String k = ((Kinetics)iter.next()).toChemkinString(Hrxn,p_temperature, true);
			result.append(strucString + "  " + k + "\nDUP");
			while (iter.hasNext()){
				
				k = ((Kinetics)iter.next()).toChemkinString(Hrxn,p_temperature, true);
				
				result.append("\n"+strucString + "  " + k + "\nDUP");
				
			}
		}
		else {
			String k = getKinetics().toChemkinString(Hrxn,p_temperature, true);
			result.append(strucString+ " " + k);
		}
		
		ChemkinString = result.toString();
      return result.toString();

      //#]
  }

	public String toRestartString(Temperature p_temperature) {
      //#[ operation toChemkinString()
	
      String result = getStructure().toRestartString(hasReverseReaction())+ " "+getStructure().direction + " "+getStructure().redundancy;
      String k = getKinetics().toChemkinString(calculateHrxn(p_temperature),p_temperature, true);
      result = result + " " + k;

      return result;

      //#]
  }

  //## operation toFullString()
  public String toFullString() {
      //#[ operation toFullString()
      return getStructure().toString() + getKinetics().toString() + getComments().toString();



      //#]
  }

 
  //## operation toString()
  public String toString(Temperature p_temperature) {
      //#[ operation toString()
      Kinetics k = getKinetics();
      String kString = k.toChemkinString(calculateHrxn(p_temperature),p_temperature,false);
      
      return getStructure().toString() + '\t' + kString;
      //#]
  }

  public String toString() {
      //#[ operation toString()
	  Temperature p_temperature = Global.temperature;
      Kinetics k = getKinetics();
      String kString = k.toChemkinString(calculateHrxn(p_temperature),p_temperature,false);
      
      return getStructure().toString() + '\t' + kString;
      //#]
  }
  
  public static double getBIMOLECULAR_RATE_UPPER() {
      return BIMOLECULAR_RATE_UPPER;
  }

  public static double getUNIMOLECULAR_RATE_UPPER() {
      return UNIMOLECULAR_RATE_UPPER;
  }

  public void setComments(String p_comments) {
      comments = p_comments;
  }

  public Reaction getReverseReaction() {
      return reverseReaction;
  }

  public void setKinetics(Kinetics p_kinetics) {
      kinetics = p_kinetics;
  }
	
	public void addAdditionalKinetics(Kinetics p_kinetics, int red) {
		if (finalized)
			return;
		if (p_kinetics == null)
			return;
		if (kinetics == null){
			kinetics = p_kinetics;
			structure.redundancy = 1;
		}
		else if (kinetics.equals(p_kinetics)){
			structure.increaseRedundancy(red);
		}
		else if (additionalKinetics == null)
			if (p_kinetics.calculateRate(Global.temperature) > kinetics.calculateRate(Global.temperature)){
				additionalKinetics = kinetics;
				kinetics = p_kinetics;
				structure.redundancy = 1;
			}
			else additionalKinetics = p_kinetics;
		else if (additionalKinetics.equals(p_kinetics))
			return;
		else {
			if (p_kinetics.calculateRate(Global.temperature) > kinetics.calculateRate(Global.temperature)){
				additionalKinetics = kinetics;
				kinetics = p_kinetics;
				System.out.println("More than 2 kinetics provided for reaction " + structure.toChemkinString(true));
				System.out.println("Ignoring the rate constant " + additionalKinetics.toString() );
			
			}
			else if (p_kinetics.calculateRate(Global.temperature) < additionalKinetics.calculateRate(Global.temperature)){
				System.out.println("More than 2 kinetics provided for reaction " + structure.toChemkinString(true));
				System.out.println("Ignoring the rate constant " + p_kinetics.toString() );
			}
			else {
				System.out.println("More than 2 kinetics provided for reaction " + structure.toChemkinString(true));
				System.out.println("Ignoring the rate constant " + additionalKinetics.toString() );
				additionalKinetics = p_kinetics;
			}
		}
	}
	
	public boolean hasAdditionalKinetics(){
		return (additionalKinetics != null);
	}
	
	public int totalNumberOfKinetics(){
		if (hasAdditionalKinetics())
			return 2;
		else
			return 1;
	}
	
	public HashSet getAllKinetics(){
		HashSet allKinetics = new HashSet();
		allKinetics.add(kinetics.multiply(structure.redundancy));
		if ( hasAdditionalKinetics()){
			allKinetics.add(additionalKinetics.multiply(structure.redundancy));
			
		}
		
		return allKinetics;	
	}
	
	

	public void setFinalized(boolean p_finalized) {
		finalized = p_finalized;
		return;
	}
	
  public Structure getStructure() {
      return structure;
  }

  public void setStructure(Structure p_Structure) {
      structure = p_Structure;
  }

}
/*********************************************************************
	File Path	: RMG\RMG\jing\rxn\Reaction.java
*********************************************************************/

