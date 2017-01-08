package ch.fhnw.helper;

public class GeneralResponse<T> {
    
    private ErrorResponse error;
    private T response;
    
    
    public GeneralResponse() {}
    
    
    public GeneralResponse(ErrorResponse error, T response) {
        this.error = error;
        this.response = response;
    }

    
    public boolean isError() {
        return this.error != null;
    }

    public ErrorResponse getError() {
        return error;
    }


    public void setError(ErrorResponse error) {
        this.error = error;
    }


    public T getResponse() {
        return response;
    }


    public void setResponse(T response) {
        this.response = response;
    }
    

}
