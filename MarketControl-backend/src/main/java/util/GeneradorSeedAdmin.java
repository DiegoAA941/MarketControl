package util;

/**
 * Herramienta de una sola vez para regenerar el INSERT cifrado del usuario
 * administrador semilla (Base de datos/BaseDeDatosMarketControl.txt).
 *
 * Uso: java -cp "target/classes;<classpath>" util.GeneradorSeedAdmin <DNI> <Email>
 * Requiere MC_CLAVE_CIFRADO configurada en el entorno antes de correr.
 */
public class GeneradorSeedAdmin {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java util.GeneradorSeedAdmin <DNI> <Email>");
            return;
        }
        String dni = args[0];
        String email = args[1];

        System.out.println("DNI cifrado:   " + CryptoUtil.cifrar(dni));
        System.out.println("DniHash:       " + CryptoUtil.indiceCiego(dni));
        System.out.println("Email cifrado: " + CryptoUtil.cifrar(email));
        System.out.println("EmailHash:     " + CryptoUtil.indiceCiego(email));
    }
}
