
document.addEventListener("DOMContentLoaded", () => {

    // ---------------------------------------
    // Mobile sidebar navigation
    // ---------------------------------------

    const menuButton = document.querySelector("[data-sidebar-toggle]");
    const overlay = document.querySelector("[data-sidebar-close]");

    function setSidebar(open) {
        document.body.classList.toggle("sidebar-open", open);

        if (menuButton) {
            menuButton.setAttribute("aria-expanded", String(open));
        }
    }

    if (menuButton) {
        menuButton.addEventListener("click", () => {
            const open = !document.body.classList.contains("sidebar-open");
            setSidebar(open);
        });
    }

    if (overlay) {
        overlay.addEventListener("click", () => {
            setSidebar(false);
        });
    }

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            setSidebar(false);
        }
    });

    // ---------------------------------------
    // Dismiss success / error notifications
    // ---------------------------------------

    document.querySelectorAll("[data-dismiss-alert]").forEach(button => {
        button.addEventListener("click", () => {
            const notice = button.closest(".notice");

            if (notice) {
                notice.remove();
            }
        });
    });

    // ---------------------------------------
    // Transfer amount preview
    // ---------------------------------------

    const transferForm = document.getElementById("transferForm");

    if (transferForm) {

        const amountInput = document.getElementById("amount");
        const amountElement = document.getElementById("previewAmount");
        const feeElement = document.getElementById("previewFee");
        const totalElement = document.getElementById("previewTotal");

        const feeEnabled = transferForm.dataset.feeEnabled === "true";

        const formatter = new Intl.NumberFormat("en-LK", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });

        function formatMoney(value) {
            return "LKR " + formatter.format(value);
        }

        function updateTransferPreview() {

            if (!amountInput) return;

            const rawAmount = Number(amountInput.value);
            const amount = Number.isFinite(rawAmount)
                ? Math.max(0, rawAmount)
                : 0;

            const fee = feeEnabled
                ? Math.round((amount * 0.01 + Number.EPSILON) * 100) / 100
                : 0;

            const total = amount + fee;

            if (amountElement) {
                amountElement.textContent = formatMoney(amount);
            }

            if (feeElement) {
                feeElement.textContent = formatMoney(fee);
            }

            if (totalElement) {
                totalElement.textContent = formatMoney(total);
            }
        }

        if (amountInput) {
            amountInput.addEventListener("input", updateTransferPreview);
            amountInput.addEventListener("change", updateTransferPreview);
        }

        updateTransferPreview();
    }
});
