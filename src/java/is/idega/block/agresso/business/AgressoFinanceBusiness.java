package is.idega.block.agresso.business;

import is.idega.block.agresso.dao.AgressoDAO;
import is.idega.block.agresso.data.AgressoFinanceEntry;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.core.business.DefaultSpringBean;
import com.idega.core.persistence.Param;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.expression.ELUtil;

@Service("agressoFinanceBusiness")
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class AgressoFinanceBusiness extends DefaultSpringBean {

	@Autowired
	private AgressoDAO agressoDAO;

	public Long createParkingEntry(String entryType, Date ticketDate, String user, Integer amount, String info,
			String registrationNumber, String permanentNumber, String carType,
			String owner, String ticketNumber, String ticketOfficer,
			String streetName, String streetNumber, String streetDescription,
			String meterNumber, String invoiceNumber) {
		
		try {
			IWTimestamp paymentDate = new IWTimestamp(ticketDate);
			paymentDate.addDays(14);
			
			return getAgressoDAO().addFinanceEntryParking("PARKING", user, amount,
					paymentDate.getTimestamp(),ticketDate, info, registrationNumber, permanentNumber, carType, owner, ticketNumber, ticketOfficer, streetName,
					streetNumber, streetDescription, meterNumber, invoiceNumber);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error creating parking entry! Ticket number: " + ticketNumber, e);
		}
		
		return null;
	}
	
	public boolean deleteParkingEntry(Long entryID) {
		try {
			return getAgressoDAO().deleteFinanceEntry(entryID);
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error deleting parking entry: " + entryID, e);
		}
		
		return false;
	}

	protected AgressoDAO getAgressoDAO() {
		if (agressoDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return agressoDAO;
	}
	
	/**
	 * 
	 * @param ticketNumber, required
	 * @param isProtested, required otherwise isProtested and protestDate will be nulled in the db
	 * @param rulingResult, if not supplied rulingResult and rulingDate will be nulled.
	 */
	public void updateTicketProtestInfo(String ticketNumber, boolean isProtested, String status, String reason, String explanation, Timestamp rullingDate) {
		List<AgressoFinanceEntry> entries = null;
		try {
			entries = getAgressoDAO().getResultList(AgressoFinanceEntry.NAMED_QUERY_FIND_BY_TICKET_NUMBER, AgressoFinanceEntry.class,
					new Param("ticketNumber", ticketNumber));
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Error while trying to find entry in DB table '" + AgressoFinanceEntry.ENTITY_NAME + "' by ticket number: " + ticketNumber, e);
			return;
		}
		if (ListUtil.isEmpty(entries)) {
			getLogger().warning("Unable to find entry in DB table '" + AgressoFinanceEntry.ENTITY_NAME + "' by ticket number: " + ticketNumber);
			return;
		}
		
		if (entries.size() > 1) {
			getLogger().warning("Founf multiple entries for the same ticket number (" + ticketNumber + "): " + entries);
		}
		
		try {
			AgressoFinanceEntry entry = entries.iterator().next();
			if (isProtested) {
				String alreadyProtested = entry.getIsProtested();
				if (!"1".equals(alreadyProtested)) {
					entry.setIsProtested("1");
					entry.setProtestedDate(IWTimestamp.RightNow().getTimestamp());
				}
			} else {
				entry.setIsProtested(null);
				entry.setProtestedDate(null);
			}
			
			entry.setRulingResult(status);
			entry.setRulingResultDate(rullingDate);
			entry.setRullingPredefinedText(reason);
			entry.setRullingExplanationText(explanation);
			
			getAgressoDAO().persist(entry);
		} catch (Exception e) {
			String message = "Error while updating agresso entry: ticket number: " + ticketNumber + ", status: " + status + ", reason: " + reason +	", explanation: " +
				explanation + ", rulling date: " + rullingDate;
			getLogger().log(Level.WARNING, message, e);
			throw new RuntimeException(message, e);
		}
	}
}