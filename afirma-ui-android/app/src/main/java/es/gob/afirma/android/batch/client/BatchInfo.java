package es.gob.afirma.android.batch.client;

import org.json.JSONException;

import java.util.List;

public interface BatchInfo {

	void updateResults(List<BatchDataResult> results) throws JSONException;

	String getInfoString();
}
