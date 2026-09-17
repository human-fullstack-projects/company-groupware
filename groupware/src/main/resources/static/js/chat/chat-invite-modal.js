// 동료 초대 모달 공통 로직 (chat/list.html, chat/detail.html 공용)
(function () {
    const veil = document.getElementById('inviteModalVeil');
    const filterInput = document.getElementById('inviteFilterInput');
    const list = document.getElementById('inviteEmployeeList');
    const emptyMessage = document.getElementById('inviteEmptyMessage');
    const cancelBtn = document.getElementById('inviteCancelBtn');
    const confirmBtn = document.getElementById('inviteConfirmBtn');

    let employees = [];
    const selectedIds = new Set();
    let onConfirm = null;

    function renderList(filterText) {
        list.innerHTML = '';
        const filtered = employees.filter(e => !filterText || e.emplName.includes(filterText));

        if (filtered.length === 0) {
            emptyMessage.style.display = 'block';
            return;
        }
        emptyMessage.style.display = 'none';

        filtered.forEach(e => {
            const li = document.createElement('li');
            li.className = 'invite-popup__item';

            const label = document.createElement('label');
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.value = e.emplId;
            checkbox.checked = selectedIds.has(e.emplId);
            checkbox.addEventListener('change', () => {
                if (checkbox.checked) {
                    selectedIds.add(e.emplId);
                } else {
                    selectedIds.delete(e.emplId);
                }
            });

            label.appendChild(checkbox);
            label.appendChild(document.createTextNode(
                ' ' + e.emplName + (e.deptName ? ' (' + e.deptName + ')' : '')));
            li.appendChild(label);
            list.appendChild(li);
        });
    }

    async function loadEmployees(roomId) {
        const url = '/api/employees' + (roomId ? ('?roomId=' + encodeURIComponent(roomId)) : '');
        const res = await fetch(url);
        if (!res.ok) {
            alert('직원 목록을 불러오지 못했습니다.');
            return;
        }
        employees = await res.json();
        renderList('');
    }

    function closeModal() {
        veil.classList.remove('is-open');
        onConfirm = null;
        selectedIds.clear();
        filterInput.value = '';
    }

    filterInput.addEventListener('input', (event) => {
        renderList(event.target.value.trim());
    });

    cancelBtn.addEventListener('click', closeModal);

    veil.addEventListener('click', (event) => {
        if (event.target === veil) {
            closeModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && veil.classList.contains('is-open')) {
            closeModal();
        }
    });

    confirmBtn.addEventListener('click', () => {
        if (selectedIds.size === 0) {
            alert('초대할 직원을 한 명 이상 선택하세요.');
            return;
        }
        const ids = Array.from(selectedIds);
        const callback = onConfirm;
        closeModal();
        if (callback) {
            callback(ids);
        }
    });

    // roomId(없으면 새 채팅방 생성용) 와 선택 완료 콜백을 받아 모달을 연다
    window.ChatInvite = {
        open: function (roomId, callback) {
            onConfirm = callback;
            veil.classList.add('is-open');
            filterInput.focus();
            loadEmployees(roomId);
        }
    };
})();
