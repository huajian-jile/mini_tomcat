// Mini Tomcat 静态资源演示
document.addEventListener('DOMContentLoaded', function() {
  const cards = document.querySelectorAll('.card');
  cards.forEach((card, i) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(10px)';
    card.style.animation = `fadeIn 0.4s ease ${i * 0.08}s forwards`;
  });
});
