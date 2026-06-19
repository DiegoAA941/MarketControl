const OJO_ABIERTO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></svg>';
const OJO_CERRADO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9.9 4.24A9.1 9.1 0 0 1 12 4c6.5 0 10 7 10 7a13.2 13.2 0 0 1-2.4 3.3"/><path d="M6.6 6.6A13.2 13.2 0 0 0 2 12s3.5 7 10 7a9.1 9.1 0 0 0 3.4-.6"/><path d="M14.1 14.1A3 3 0 0 1 9.9 9.9"/><path d="M2 2l20 20"/></svg>';

document.querySelectorAll('.ojo').forEach(btn => {
    btn.innerHTML = OJO_ABIERTO;
    btn.addEventListener('click', () => {
        const input = document.getElementById(btn.dataset.alternar);
        const mostrar = input.type === 'password';
        input.type = mostrar ? 'text' : 'password';
        btn.innerHTML = mostrar ? OJO_CERRADO : OJO_ABIERTO;
        btn.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
    });
});

const pestanas = document.querySelectorAll('.pestana');
const vistas = document.querySelectorAll('.vista');

pestanas.forEach(pestana => {
    pestana.addEventListener('click', () => {
        pestanas.forEach(p => p.setAttribute('aria-selected', p === pestana));
        vistas.forEach(v => v.classList.toggle('activa', v.id === pestana.dataset.vista));
    });
});

function notificar(id, texto, exito) {
    const elemento = document.getElementById(id);
    elemento.textContent = texto;
    elemento.className = 'mensaje mostrar ' + (exito ? 'exito' : 'error');
}

const CUENTAS = { admin: 'admin123', empleado1: 'emp123' };

document.getElementById('acceso').addEventListener('submit', e => {
    e.preventDefault();
    const usuario = document.getElementById('a-usuario').value.trim();
    const clave = document.getElementById('a-clave').value;

    if (!usuario || !clave) return notificar('acceso-msj', 'Completa usuario y contraseña.', false);

    if (CUENTAS[usuario] && CUENTAS[usuario] === clave) {
        notificar('acceso-msj', 'Acceso correcto.', true);
    } else {
        notificar('acceso-msj', 'Usuario o contraseña incorrectos.', false);
    }
});

document.getElementById('registro').addEventListener('submit', e => {
    e.preventDefault();
    const nombre = document.getElementById('r-nombre').value.trim();
    const usuario = document.getElementById('r-usuario').value.trim();
    const clave1 = document.getElementById('r-clave').value;
    const clave2 = document.getElementById('r-clave2').value;

    if (!nombre || !usuario || !clave1 || !clave2) return notificar('registro-msj', 'Completa los campos obligatorios.', false);
    if (usuario.length < 4) return notificar('registro-msj', 'El usuario necesita al menos 4 caracteres.', false);
    if (clave1.length < 6) return notificar('registro-msj', 'La contraseña necesita al menos 6 caracteres.', false);
    if (clave1 !== clave2) return notificar('registro-msj', 'Las contraseñas no coinciden.', false);

    notificar('registro-msj', 'Cuenta creada. Ya puedes iniciar sesión.', true);
});

document.getElementById('restablecer').addEventListener('submit', e => {
    e.preventDefault();
    const usuario = document.getElementById('x-usuario').value.trim();
    const clave1 = document.getElementById('x-clave').value;
    const clave2 = document.getElementById('x-clave2').value;

    if (!usuario || !clave1 || !clave2) return notificar('restablecer-msj', 'Completa todos los campos.', false);
    if (clave1.length < 6) return notificar('restablecer-msj', 'La contraseña necesita al menos 6 caracteres.', false);
    if (clave1 !== clave2) return notificar('restablecer-msj', 'Las contraseñas no coinciden.', false);

    notificar('restablecer-msj', 'Contraseña actualizada correctamente.', true);
});