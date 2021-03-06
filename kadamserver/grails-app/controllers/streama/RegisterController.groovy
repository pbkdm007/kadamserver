package streama


import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.annotation.Secured
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.WebAttributes

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import javax.servlet.http.Cookie

import java.util.Map
import java.util.Random
import java.util.Enumeration
import java.util.HashMap
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import grails.transaction.Transactional

import com.instamojo.wrapper.exception.ConnectionException
import com.instamojo.wrapper.exception.HTTPException
import com.instamojo.wrapper.model.PaymentOrder
import com.instamojo.wrapper.model.PaymentOrderResponse
import com.instamojo.wrapper.api.ApiContext
import com.instamojo.wrapper.api.Instamojo
import com.instamojo.wrapper.api.InstamojoImpl

@Secured('permitAll')
class RegisterController {

  /** Dependency injection for the authenticationTrustResolver. */
  AuthenticationTrustResolver authenticationTrustResolver

  /** Dependency injection for the springSecurityService. */
  def springSecurityService

  /** Dependency injection for the settingsService. */
  def settingsService
  
  def error

  //@Transactional
  def register() {
	String username = params.username
    def isInvite = true
    def result = [:]
    
    if(empty(username))
    {
      String message = "Please enter valid username."
      String usernamespanclass = "ion-close form-control-feedback"
      String hasusernameclass = "has-error has-feedback"
      String postUrl = request.contextPath + '/register/register'
      render view: 'registration', model: [postUrl: postUrl, message: message, 
      usernamespanclass: usernamespanclass, hasusernameclass: hasusernameclass]
      return
    }

    if (User.findByUsername(username)) {
      String message = "Username already exists."
      String usernamespanclass = "ion-close form-control-feedback"
      String hasusernameclass = "has-error has-feedback"
      String postUrl = request.contextPath + '/register/register'
      render view: 'registration', model: [postUrl: postUrl, message: message, 
      usernamespanclass: usernamespanclass, hasusernameclass: hasusernameclass]
    } else {
    String hasusernameclass = "has-success has-feedback"
    		if (empty(params.password) || empty(params.password2)) {
    			String message = "The password can not be empty"
    			String passwordspanclass = "ion-close form-control-feedback"
                String password2spanclass = "ion-close form-control-feedback"
                String haspasswordclass = "has-error has-feedback"
                String postUrl = request.contextPath + '/register/register'
    			render view: 'registration', model: [postUrl: postUrl, message: message, 
    			passwordspanclass: passwordspanclass, password2spanclass: password2spanclass, 
    			hasusernameclass: hasusernameclass, haspasswordclass: haspasswordclass]
    		}
    		else if (params.password.size() < 6) {
    			String message = "The password must be at least 6 characters long"
    			String passwordspanclass = "ion-close form-control-feedback"
                String password2spanclass = "ion-close form-control-feedback"
                String haspasswordclass = "has-error has-feedback"
                String postUrl = request.contextPath + '/register/register'
    			render view: 'registration', model: [postUrl: postUrl, message: message, 
    			passwordspanclass: passwordspanclass, password2spanclass: password2spanclass, 
    			hasusernameclass: hasusernameclass, haspasswordclass: haspasswordclass]
    		}
    		else if (params.password != params.password2) {
                String message = "The passwords need to match"
                String passwordspanclass = "ion-close form-control-feedback"
                String password2spanclass = "ion-close form-control-feedback"
                String haspasswordclass = "has-error has-feedback"
                String postUrl = request.contextPath + '/register/register'
    			render view: 'registration', model: [postUrl: postUrl, message: message, 
    			passwordspanclass: passwordspanclass, password2spanclass: password2spanclass, 
    			hasusernameclass: hasusernameclass, haspasswordclass: haspasswordclass]
            }
            else if("0".equals(params.amount)) {
            User.withTransaction {
            	User user = new User(
                        username: params.username,
                        password: params.password,
                        fullName: params.firstname,
                        phone: params.phone
                )
                user.validate()
			    if (user.hasErrors()) {
			      render status: NOT_ACCEPTABLE
			      return
			    }
			    
			    Calendar now = Calendar.getInstance()
			    
			    now.add(Calendar.DAY_OF_MONTH,5)
			    
			    Date expiryDate = now.getTime()
	
				user.expiryDate = expiryDate
				user.amountPaid = params.amount
				user.accountExpired = false
				user.enabled = true
			
			    user.save flush: true
			
			    UserRole.removeAll(user)
			    
			    Role role = Role.get(2)
				UserRole.create(user, role)
				}
			    
			    render view: 'success'
            }
            else if(!"10".equals(params.amount) && !"50".equals(params.amount) && !"100".equals(params.amount)) {
            	String message = "The selected plan is invalid"
            	String postUrl = request.contextPath + '/register/register'
    			render view: 'registration', model: [postUrl: postUrl, message: message]
            }
            else {
            	String usernamespanclass = "ion-checkmark form-control-feedback"
            	String passwordspanclass = "ion-checkmark form-control-feedback"
            	String password2spanclass = "ion-checkmark form-control-feedback"
            	String haspasswordclass = "has-success has-feedback"
            	User user = new User(
                        username: params.username,
                        password: params.password,
                        fullName: params.firstname,
                        phone: params.phone
                )
                user.validate()
			    if (user.hasErrors()) {
			      render status: NOT_ACCEPTABLE
			      return
			    }
			
			    user.save flush: true
			    
			    /*UserRole.removeAll(userInstance)
			
				Role role = Role.get(2)
      			UserRole.create(userInstance, role)*/
      			
			    //UserRole.create(user, Role.findByAuthority("ROLE_CONTENT_MANAGER"))
			    
			    /**Cookie cookie = new Cookie("myCookie",username)
				cookie.maxAge = -1
				response.addCookie(cookie)*/
			    
			    //response.setHeader 'Authorization' , 'D2GolFkwmvomSHkZ9GAVMQq2soPOtBixMj2E3Sb5IxI='
			    
			    //pay()
			    
			    payViaInstamojo()
			    
			    /** redirect(url: "https://www.payumoney.com/sandbox/paybypayumoney/#/898B9046B7F1201205DA2DBCC4083632")
			     redirect(url: "https://www.payumoney.com/paybypayumoney/#/0777B13D79F428A2793B1D81AAD66355") */
            }
    }
  }
  
  public boolean empty(String s) {
        if (s == null || s.trim().equals("")) {
            return true;
        } else {
            return false;
        }
    }

    public String hashCal(String type,String str){
		byte[] hashseq=str.getBytes();
		StringBuffer hexString = new StringBuffer();
		try{
		MessageDigest algorithm = MessageDigest.getInstance(type);
		algorithm.reset();
		algorithm.update(hashseq);
		byte[] messageDigest = algorithm.digest();
            
		for (int i=0;i<messageDigest.length;i++) {
			String hex=Integer.toHexString(0xFF & messageDigest[i]);
			if(hex.length()==1) hexString.append("0");
			hexString.append(hex);
		}
			
		}catch(NoSuchAlgorithmException nsae){ }
		
		return hexString.toString();

	}

    public Map<String, String> hashCalMethod()
            {
        response.setContentType("text/html;charset=UTF-8");
		String key = "";
        String salt = "haqtx6QnvO";
        String action1 = "";
        String base_url = "https://sandboxsecure.payu.in";
        error = 0;
        String hashString = "";
        Enumeration paramNames = request.getParameterNames();
        Map<String, String> tempparams = new HashMap<String, String>();
        Map<String, String> urlParams = new HashMap<String, String>();
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            tempparams.put(paramName, paramValue);
        }
        String txnid = "";
        if (empty(tempparams.get("txnid"))) {
            Random rand = new Random();
            String rndm = Integer.toString(rand.nextInt()) + (System.currentTimeMillis() / 1000L);
            txnid = rndm;
            tempparams.remove("txnid");
            txnid = hashCal("SHA-256", rndm).substring(0, 20);
            tempparams.put("txnid", txnid);
        } else {
            txnid = tempparams.get("txnid");
        }
        
        tempparams.put("udf2", txnid);
        String amount = tempparams.get("amount")
        
        if("10".equals(amount)) {
        tempparams.put("productinfo", "1 month plan")
        } else if("50".equals(amount)) {
        tempparams.put("productinfo", "6 month plan")
        } else if("100".equals(amount)) {
        tempparams.put("productinfo", "1 year plan")
        }
        
        tempparams.put("email", params.username)
        
        String hash = "";
        String otherPostParamSeq = "phone|surl|furl|lastname|curl|address1|address2|city|state|country|zipcode|pg";
        String hashSequence = "key|txnid|amount|productinfo|firstname|email|udf1|udf2|udf3|udf4|udf5";
        if (empty(tempparams.get("hash")) && tempparams.size() > 0) {
            if (empty(tempparams.get("key")) || empty(txnid) || empty(tempparams.get("amount")) || empty(tempparams.get("firstname")) || empty(tempparams.get("email")) || empty(tempparams.get("phone")) || empty(tempparams.get("productinfo")) || empty(tempparams.get("surl")) || empty(tempparams.get("furl")) || empty(tempparams.get("service_provider"))) {
                error = 1;
            } else {
                
                String[] hashVarSeq = hashSequence.split("\\|");
                for (String part : hashVarSeq) {
                    hashString = (empty(tempparams.get(part))) ? hashString.concat("") : hashString.concat(tempparams.get(part).trim());
                    hashString = hashString.concat("|");
                    urlParams.put(part, empty(tempparams.get(part)) ? "" : tempparams.get(part).trim());
                }
                hashString = hashString.concat("|||||");
                hashString = hashString.concat(salt);
                hash = hashCal("SHA-512", hashString);
                action1 = base_url.concat("/_payment");
                String[] otherPostParamVarSeq = otherPostParamSeq.split("\\|");
                for (String part : otherPostParamVarSeq) {
                    urlParams.put(part, empty(tempparams.get(part)) ? "" : tempparams.get(part).trim());
                }

            }
        } else if (!empty(tempparams.get("hash"))) {
            hash = tempparams.get("hash");
            action1 = base_url.concat("/_payment");
        }

		urlParams.put("key","dKqf7Mff")
		urlParams.put("txnid", txnid);
        urlParams.put("hash", hash);
        urlParams.put("action", action1);
        urlParams.put("hashString", hashString);
        return urlParams;
    }
    
    def pay() {
    	Map<String, String> values = hashCalMethod();
    	render view: 'payuform', model: [tempparams: values]
    }
  
  /** Show the register page. */
  def show() {

    /** Check if anonymous access is enabled, to avoid login 
    User anonymous = User.findByUsername("anonymous")
    springSecurityService.reauthenticate(anonymous.username,anonymous.password) **/

    def conf = getConf()

	String postUrl = request.contextPath + '/register/register'
    render view: 'registration', model: [postUrl: postUrl]
    
    /** redirect(uri: '/#/register') */
  }
  
  def payorrenew() {
  User user = User.findByUsername(params.username)
  String username = params.username
  String firstname = params.firstname
  String phone = params.phone
  	if("0".equals(params.amount) && "0".equals(user.amountPaid)) {
  		
  		flash.message = "You have already used free plan"
  		String postUrl = request.contextPath + '/register/payorrenew'
  		render view: 'payorrenew', params: params, model: [postUrl: postUrl, username: username, 
    	firstname: firstname, phone: phone]
  		return
  		
  	} 
  	else 
  	{
	  	if(!"10".equals(params.amount) && !"50".equals(params.amount) && !"100".equals(params.amount)) {
	       flash.message = "The selected plan is invalid"
	       String postUrl = request.contextPath + '/register/payorrenew'
	       render view: 'payorrenew', params: params, model: [postUrl: postUrl, username: username, 
    		firstname: firstname, phone: phone]
	       return
	    }
	  	/**def username = params.username
	    Cookie cookie = new Cookie("myCookie",username)
		cookie.maxAge = -1
		response.addCookie(cookie)*/
		pay()
	}
  }
  
  @Transactional
  def success() {
  
  	PaymentOrder paymentOrder=null;

	try {
	ApiContext context = ApiContext.create("JpCABmIADQ2VC6kMXzdENF5vfEkrZEfuVmfHN9Qe", 
	"Iq7eB8dUoyAnLBHZ43QZoERmT1wbxJG8S56nlucP0GyUQLlMXoDKh6BUd7SBRUoJz7fJTcDIR8hPrbMgFjLFvZcjgffNLLj0qtvsAeyle8j63MuYPxF4XwNeQ6dn1DvG", ApiContext.Mode.LIVE);
	Instamojo api = new InstamojoImpl(context);
    paymentOrder = api.getPaymentOrder(params.id);
    //System.out.println(paymentOrder.getId());
    //System.out.println(paymentOrder.getStatus());
	
	} catch (HTTPException e) {
	    System.out.println(e.getStatusCode());
	    System.out.println(e.getMessage());
	    System.out.println(e.getJsonPayload());
	
	} catch (ConnectionException e) {
	    System.out.println(e.getMessage());
	}
    
    if(paymentOrder!=null && "Credit".equals(params.payment_status)) {
    String username = paymentOrder.email
    
    String amount = String.valueOf(paymentOrder.amount)
    
    BigDecimal stripedVal = new BigDecimal(amount).stripTrailingZeros();
    
    amount = stripedVal.toPlainString()
    
    //System.out.println(username);
    //System.out.println(amount);

	User userInstance = User.findByUsername(username)
	
	if(userInstance != null) {
	Calendar now = Calendar.getInstance()
	
	if("10".equals(amount)) {
		now.add(Calendar.MONTH,1)
	} else if("50".equals(amount)) {
		now.add(Calendar.MONTH,6)
	} else if("100".equals(amount)) {
		now.add(Calendar.MONTH,12)
	}
	
	Date expiryDate = now.getTime()
	
	userInstance.expiryDate = expiryDate
	userInstance.amountPaid = amount
	userInstance.accountExpired = false
	userInstance.enabled = true
	
	userInstance.save flush: true
	
	UserRole.removeAll(userInstance)
			
	Role role = Role.get(2)
	UserRole.create(userInstance, role)
	
	//UserRole.create(userInstance, Role.findByAuthority("ROLE_CONTENT_MANAGER"))
	} else {
		redirect action: 'error'
		return
	}
	}
	
    render view: 'success'
  }
  
  @Transactional
  def successwebhook() {

    if(params!=null && params.buyer!=null && "Credit".equals(params.status)) {
    String username = params.buyer
    
    String amount = params.amount

	User userInstance = User.findByUsername(username)
	
	if(userInstance != null) {
	Calendar now = Calendar.getInstance()
	
	if("10.0".equals(amount)) {
		now.add(Calendar.MONTH,1)
	} else if("50.0".equals(amount)) {
		now.add(Calendar.MONTH,6)
	} else if("100.0".equals(amount)) {
		now.add(Calendar.MONTH,12)
	} else {
		userInstance.enabled = false
	}
	
	Date expiryDate = now.getTime()
	
	userInstance.expiryDate = expiryDate
	userInstance.amountPaid = amount
	userInstance.accountExpired = false
	userInstance.enabled = true
	
	userInstance.save flush: true
	
	UserRole.removeAll(userInstance)
			
	Role role = Role.get(2)
	UserRole.create(userInstance, role)
	
	//UserRole.create(userInstance, Role.findByAuthority("ROLE_CONTENT_MANAGER"))
	}
	}
	
  }
  
  def error() {

    def conf = getConf()
    
	String postUrl = request.contextPath + '/register/register'
    render view: 'registration', model: [postUrl: postUrl, message: "Error"]
  }
  
  def payViaInstamojo() {
	   /*
	 * Get a reference to the instamojo api
	 */
	ApiContext context = ApiContext.create("JpCABmIADQ2VC6kMXzdENF5vfEkrZEfuVmfHN9Qe", 
	"Iq7eB8dUoyAnLBHZ43QZoERmT1wbxJG8S56nlucP0GyUQLlMXoDKh6BUd7SBRUoJz7fJTcDIR8hPrbMgFjLFvZcjgffNLLj0qtvsAeyle8j63MuYPxF4XwNeQ6dn1DvG", ApiContext.Mode.LIVE);
	Instamojo api = new InstamojoImpl(context);
	
	/*
	 * Create a new payment order
	 */
	PaymentOrder order = new PaymentOrder();
	order.setName(params.firstname);
	order.setEmail(params.username);
	order.setPhone(params.phone);
	order.setCurrency("INR");
	order.setAmount(Double.parseDouble(params.amount));
	order.setDescription("This is a test transaction.");
	order.setRedirectUrl(params.surl);
	order.setWebhookUrl(params.surl+"webhook");
	
	Random rand = new Random();
    String rndm = Integer.toString(rand.nextInt()) + (System.currentTimeMillis() / 1000L);
    String txnid = hashCal("SHA-256", rndm).substring(0, 20);
	
	order.setTransactionId(txnid);
	
	try {
	    PaymentOrderResponse paymentOrderResponse = api.createPaymentOrder(order);
	    redirect(url: paymentOrderResponse.getPaymentOptions().getPaymentUrl());
	    System.out.println(paymentOrderResponse.getPaymentOrder().getStatus());
	
	} catch (HTTPException e) {
	    System.out.println(e.getStatusCode());
	    System.out.println(e.getMessage());
	    System.out.println(e.getJsonPayload());
	throw e;
	} catch (ConnectionException e) {
	    System.out.println(e.getMessage());
	    throw e;
	}
  }

  /** The redirect action for Ajax requests. */
  def authAjax() {
    response.setHeader 'Location', conf.auth.ajaxLoginFormUrl
    render(status: HttpServletResponse.SC_UNAUTHORIZED, text: 'Unauthorized')
  }

  /** Show denied page. */
  def denied() {
    if (springSecurityService.isLoggedIn() && authenticationTrustResolver.isRememberMe(authentication)) {
      // have cookie but the page is guarded with IS_AUTHENTICATED_FULLY (or the equivalent expression)
      redirect action: 'full', params: params
      return
    }

    [gspLayout: conf.gsp.layoutDenied]
  }

  /** Login page for users with a remember-me cookie but accessing a IS_AUTHENTICATED_FULLY page. */
  def full() {
    def conf = getConf()
    render view: 'auth', params: params,
      model: [hasCookie: authenticationTrustResolver.isRememberMe(authentication),
              postUrl: request.contextPath + conf.apf.filterProcessesUrl,
              rememberMeParameter: conf.rememberMe.parameter,
              usernameParameter: conf.apf.usernameParameter,
              passwordParameter: conf.apf.passwordParameter,
              gspLayout: conf.gsp.layoutAuth]
  }

  /** Callback after a failed login. Redirects to the auth page with a warning message. */
  def authfail() {

    String msg = ''
    def exception = session[WebAttributes.AUTHENTICATION_EXCEPTION]
    if (exception) {
      if (exception instanceof AccountExpiredException) {
        msg = message(code: 'springSecurity.errors.login.expired')
      }
      else if (exception instanceof CredentialsExpiredException) {
        msg = message(code: 'springSecurity.errors.login.passwordExpired')
      }
      else if (exception instanceof DisabledException) {
        msg = message(code: 'springSecurity.errors.login.disabled')
      }
      else if (exception instanceof LockedException) {
        msg = message(code: 'springSecurity.errors.login.locked')
      }
      else {
        msg = message(code: 'springSecurity.errors.login.fail')
      }
    }

    if (springSecurityService.isAjax(request)) {
      render([error: msg] as JSON)
    }
    else {
      flash.message = msg
      redirect action: 'auth', params: params
    }
  }

  /** The Ajax success redirect url. */
  def ajaxSuccess() {
    render([success: true, username: authentication.name] as JSON)
  }

  /** The Ajax denied redirect url. */
  def ajaxDenied() {
    render([error: 'access denied'] as JSON)
  }

  protected Authentication getAuthentication() {
    SecurityContextHolder.context?.authentication
  }

  protected ConfigObject getConf() {
    SpringSecurityUtils.securityConfig
  }
}
