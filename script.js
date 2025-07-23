// script.js

// === LÓGICA DA PÁGINA DE LOGIN (executa apenas na página de login) ===
document.addEventListener("DOMContentLoaded", () => {
    // Seleciona os elementos de login. Usa 'button.login-button' para ser mais específico,
    // caso haja outros botões na página de login que não sejam para submeter o formulário.
    // Você pode precisar adicionar a classe 'login-button' ao seu botão de login no HTML.
    const emailInput = document.querySelector('input[type="email"]');
    const passwordInput = document.querySelector('input[type="password"]');
    const loginButton = document.querySelector('button'); // Se o seu botão de login não tiver uma classe específica, use apenas 'button'

    // Verifica se os elementos de login existem na página atual antes de tentar adicionar listeners
    if (loginButton && emailInput && passwordInput) {
        loginButton.addEventListener("click", (event) => {
            event.preventDefault(); // Impede o comportamento padrão de submit do formulário, que recarregaria a página

            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (email === "" || password === "") {
                alert("Por favor, preencha todos os campos.");
            } else {
                alert(`Bem-vindo ao PixFactory, ${email}!`);
                // Aqui futuramente você pode redirecionar para o painel principal
                // window.location.href = "dashboard.html";
            }
        });
    }

    // Fecha a sidebar se a tela for redimensionada para desktop enquanto ela estiver aberta
    // Isso garante que a sidebar não fique visível de forma inadequada em telas maiores
    window.addEventListener('resize', () => {
        const sidebar = document.getElementById('sidebar');
        if (sidebar && window.innerWidth > 768) {
            sidebar.classList.remove('active');
        }
    });

    // Adiciona o event listener para fechar a sidebar ao clicar em um item do menu (em mobile)
    // Garante uma melhor experiência de usuário ao navegar
    document.querySelectorAll('.sidebar nav ul li a').forEach(item => {
        item.addEventListener('click', () => {
            const sidebar = document.getElementById('sidebar');
            // Só fecha se a sidebar estiver ativa e a tela for considerada mobile
            if (sidebar && sidebar.classList.contains('active') && window.innerWidth <= 768) {
                sidebar.classList.remove('active');
            }
        });
    });
});


// === FUNÇÕES GLOBAIS (Podem ser usadas em qualquer página que este script for incluído) ===

// 🔧 Suporte via WhatsApp
// Abre uma nova aba com o chat do WhatsApp predefinido
function abrirWhatsApp() {
    // IMPORTANTE: Substitua '5581999999999' pelo seu número de WhatsApp real, incluindo o DDI e DDD
    const numeroWhatsApp = '5581999999999'; // Exemplo: DDI (55) + DDD (81) + Número (99999-9999)
    const mensagem = 'Olá! Preciso de ajuda com o PixFactory.';
    const url = `https://wa.me/${numeroWhatsApp}?text=${encodeURIComponent(mensagem)}`;
    window.open(url, '_blank');
}

// 💡 Funções para Janela de Sugestão
// Alterna a visibilidade da janela de sugestão (abre/fecha)
function abrirSugestao() {
    const janelaSugestao = document.getElementById('janela-sugestao');
    if (janelaSugestao) { // Verifica se a janela de sugestão existe na página
        // Alterna entre 'flex' (para exibir) e 'none' (para ocultar)
        janelaSugestao.style.display = janelaSugestao.style.display === 'flex' ? 'none' : 'flex';
    }
}

// Envia a sugestão e limpa o campo
function enviarSugestao() {
    const textarea = document.querySelector('#janela-sugestao textarea');
    if (!textarea) {
        alert("Erro: Campo de sugestão não encontrado.");
        return;
    }
    const sugestao = textarea.value.trim();
    if (sugestao) {
        alert('✅ Sugestão enviada com sucesso: ' + sugestao);
        textarea.value = ''; // Limpa o texto da sugestão
        abrirSugestao(); // Fecha a janela após enviar
    } else {
        alert('Por favor, digite sua sugestão antes de enviar.');
    }
}

// 🍔 Função para mostrar/esconder a sidebar em telas menores (Menu Hambúrguer)
// Esta função é chamada pelo clique no ícone do hambúrguer no HTML
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) { // Verifica se a sidebar existe na página
        sidebar.classList.toggle('active'); // Adiciona/remove a classe 'active' para controlar a visibilidade
    }
}


// === FUNÇÕES ESPECÍFICAS DE PÁGINAS (Se houver, manter aqui ou no HTML da página específica) ===

// Exemplo: Função de filtro de relatório (do relatorios.html)
// Manter aqui se você quiser que o script.js gerencie todas as funções
// ou manter no script da página relatorios.html se for muito específica e não reutilizável.
// Por clareza, estou mantendo-a aqui para demonstrar a centralização, mas ela só funcionará
// nas páginas que contiverem os elementos com id "busca" e classe "relatorio-card".
function filtrarRelatorio() {
    const termoInput = document.getElementById("busca");
    const termo = termoInput ? termoInput.value.toLowerCase() : ''; // Pega o valor ou uma string vazia se não existir
    const cards = document.querySelectorAll(".relatorio-card");

    if (termo && cards.length > 0) { // Só executa se houver um termo e cards
        cards.forEach(card => {
            const texto = card.innerText.toLowerCase();
            card.style.display = texto.includes(termo) ? "flex" : "none";
        });
    } else if (cards.length > 0 && termo === '') {
        // Se o termo for limpo, mostra todos os cards novamente
        cards.forEach(card => {
            card.style.display = "flex";
        });
    }
}