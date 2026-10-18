package cl.duoc.dsy1107.ms_rabbit_admin.dto;

public final class Reglas {
    
    private Reglas() {
    }

    public static final String NOMBRE_REGEX = "^[a-zA-Z0-9._-]{1,100}$";

    public static final String NOMBRE_MENSAJE = "solo puede tener letras, números, punto, guion y guion bajo (máximo 100 caracteres)";

}
