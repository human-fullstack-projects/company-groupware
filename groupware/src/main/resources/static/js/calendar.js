(() => {
    "use strict";

    const $ = (id) => document.getElementById(id);

    const apiUrl = $("calendarPage").dataset.api;
    const grid = $("calendarGrid");
    const status = $("calendarStatus");
    const dialog = $("scheduleDialog");
    const form = $("scheduleForm");
    const fields = $("scheduleFields");
    const saveButton = $("saveSchedule");
    const closeButton = $("closeDialog");
    const formError = $("formError");

    const titleInput = $("scheduleTitle");
    const startInput = $("scheduleStart");
    const endInput = $("scheduleEnd");
    const contentInput = $("scheduleContent");
    const cancelButton = $("cancelSchedule");

    const csrfToken =
        document.querySelector('meta[name="_csrf"]').content;

    const csrfHeader =
        document.querySelector('meta[name="_csrf_header"]').content;

    // 한국 시간 기준 오늘 날짜
    function koreanToday() {
        return new Intl.DateTimeFormat("sv-SE", {
            timeZone: "Asia/Seoul",
            year: "numeric",
            month: "2-digit",
            day: "2-digit"
        }).format(new Date());
    }

    const pad = (number) => String(number).padStart(2, "0");

    // toISOString()을 사용하지 않고 입력한 날짜를 그대로 유지
    function dateText(date) {
        return [
            date.getFullYear(),
            pad(date.getMonth() + 1),
            pad(date.getDate())
        ].join("-");
    }

    let viewDate = new Date(`${koreanToday()}T12:00:00`);
    viewDate.setDate(1);

    let selectedId = null;
    let selectedVersion = null;
    let editable = true;
    let saving = false;
    let opening = false;
    let loadSequence = 0;

    // API 공통 호출
    async function request(url, options = {}) {
        const headers = {
            Accept: "application/json"
        };

        if (options.body) {
            headers["Content-Type"] = "application/json";
        }

        // 등록·수정 요청에는 CSRF 토큰 포함
        if (options.method && options.method !== "GET") {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch(url, {
            ...options,
            headers,
            credentials: "same-origin",
            cache: "no-store"
        });

        // 로그인 화면으로 이동한 응답은 JSON으로 처리하지 않음
        if (response.redirected || response.status === 401) {
            throw new Error(
                "로그인이 만료되었을 수 있습니다. 다시 로그인해주세요."
            );
        }

        const contentType = response.headers.get("content-type") || "";
        const data = contentType.includes("json")
            ? await response.json()
            : null;

        if (!response.ok) {
            if (response.status >= 500) {
                throw new Error(
                    "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."
                );
            }

            const fallback = response.status === 403
                ? "권한이 없거나 보안 토큰이 만료되었습니다. 새로고침 후 확인해주세요."
                : "요청을 처리할 수 없습니다.";

            throw new Error(data?.message || fallback);
        }

        if (data === null) {
            throw new Error("서버 응답을 확인할 수 없습니다.");
        }

        return data;
    }

    // 월별 일정 조회
    async function loadCalendar(successMessage = "") {
        const sequence = ++loadSequence;

        const year = viewDate.getFullYear();
        const month = viewDate.getMonth();

        $("monthTitle").textContent = `${year}년 ${month + 1}월`;
        status.textContent = "일정을 불러오는 중입니다.";
        grid.replaceChildren();

        // 해당 월의 첫 주 일요일부터 6주 표시
        const rangeStart = new Date(year, month, 1);
        rangeStart.setDate(rangeStart.getDate() - rangeStart.getDay());

        const rangeEnd = new Date(rangeStart);
        rangeEnd.setDate(rangeEnd.getDate() + 42);

        const query = new URLSearchParams({
            start: `${dateText(rangeStart)}T00:00:00`,
            end: `${dateText(rangeEnd)}T00:00:00`
        });

        try {
            const schedules = await request(`${apiUrl}?${query}`);

            // 빠르게 월을 이동했을 때 이전 응답으로 덮어쓰지 않음
            if (sequence !== loadSequence) return;

            if (!Array.isArray(schedules)) {
                throw new Error("일정 목록 형식이 올바르지 않습니다.");
            }

            renderCalendar(rangeStart, month, schedules);

            status.textContent = successMessage ||
                `진행 일정 ${schedules.filter(s => !s.cancelled).length}건 · 취소 ${schedules.filter(s => s.cancelled).length}건 · 파란색은 수정 가능`;
        } catch (error) {
            if (sequence !== loadSequence) return;
            status.textContent = error.message;
        }
    }

    // 달력 표시
    function renderCalendar(rangeStart, month, schedules) {
        const today = koreanToday();

        for (let index = 0; index < 42; index++) {
            const day = new Date(rangeStart);
            day.setDate(day.getDate() + index);

            const nextDay = new Date(day);
            nextDay.setDate(nextDay.getDate() + 1);

            const dayString = dateText(day);
            const cell = document.createElement("div");
            cell.className = "calendar-day";

            if (day.getMonth() !== month) {
                cell.classList.add("other-month");
            }

            if (dayString === today) {
                cell.classList.add("today");
            }

            const dayButton = document.createElement("button");
            dayButton.type = "button";
            dayButton.className = "day-number";
            dayButton.textContent = day.getDate();
            dayButton.setAttribute(
                "aria-label",
                `${dayString} 일정 추가`
            );

            dayButton.addEventListener("click", () => {
                openCreate(dayString);
            });

            cell.appendChild(dayButton);

            // 여러 날에 걸친 일정도 해당 날짜마다 표시
            const daySchedules = schedules.filter((schedule) => {
                return new Date(schedule.startAt) < nextDay
                    && new Date(schedule.endAt) > day;
            });

            for (const schedule of daySchedules) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "schedule-item";

                if (schedule.editable) {
                    button.classList.add("editable");
                }

                if (schedule.cancelled) {
                    button.classList.add("cancelled");
                }

                const time = schedule.startAt.slice(0, 10) === dayString
                    ? schedule.startAt.slice(11, 16)
                    : "계속";

                // 사용자 입력을 HTML로 삽입하지 않음
                button.textContent =
                    `${schedule.cancelled ? "[취소] " : ""}${time} [${schedule.departmentName}] ${schedule.title}`;

                button.title = button.textContent;

                button.addEventListener("click", () => {
                    void openDetail(schedule.scheduleId);
                });

                cell.appendChild(button);
            }

            grid.appendChild(cell);
        }
    }

    // 일정 추가 창
    function openCreate(dayString) {
        if (opening || saving || dialog.open) return;

        form.reset();
        selectedId = null;
        selectedVersion = null;
        editable = true;
        cancelButton.hidden = true;

        fields.disabled = false;
        saveButton.hidden = false;
        saveButton.disabled = false;
        saveButton.textContent = "등록";

        $("dialogTitle").textContent = "일정 추가";
        $("scheduleInfo").textContent =
            "담당 부서는 현재 로그인한 직원의 소속 부서로 등록됩니다.";

        formError.textContent = "";
        startInput.value = `${dayString}T09:00`;
        endInput.value = `${dayString}T10:00`;

        dialog.showModal();
        titleInput.focus();
    }

    // 일정 상세 및 수정 창
    async function openDetail(scheduleId) {
        if (opening || saving || dialog.open) return;

        opening = true;
        status.textContent = "일정 상세 정보를 불러오는 중입니다.";

        try {
            // 목록 데이터가 아닌 최신 상세 정보를 다시 조회
            const schedule = await request(`${apiUrl}/${scheduleId}`);

            form.reset();
            selectedId = schedule.scheduleId;
            selectedVersion = schedule.version;
            editable = schedule.editable === true && !schedule.cancelled;
            cancelButton.hidden = !editable;

            titleInput.value = schedule.title;
            contentInput.value = schedule.content || "";
            startInput.value = schedule.startAt.slice(0, 16);
            endInput.value = schedule.endAt.slice(0, 16);

            fields.disabled = !editable;
            saveButton.hidden = !editable;
            saveButton.disabled = false;
            saveButton.textContent = "수정 저장";

            $("dialogTitle").textContent = schedule.cancelled ? "취소된 일정" : "일정 상세";
            $("scheduleInfo").textContent =
                `담당 부서: ${schedule.departmentName} · ` +
                `작성자: ${schedule.writerName} · ` +
                (schedule.cancelled
                    ? "취소되어 중복 검사에서 제외된 일정입니다."
                    : editable
                        ? "같은 부서 직원은 수정·취소할 수 있습니다."
                        : "다른 부서의 일정은 조회만 가능합니다.");

            formError.textContent = "";
            status.textContent = "";

            dialog.showModal();
        } catch (error) {
            status.textContent = error.message;
        } finally {
            opening = false;
        }
    }

    // 일정 등록·수정
    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        if (saving || !editable) return;

        formError.textContent = "";

        if (!form.reportValidity()) return;

        if (!titleInput.value.trim()) {
            formError.textContent = "일정 제목을 입력해주세요.";
            return;
        }

        // datetime-local 값은 동일한 형식이므로 순서 비교 가능
        if (endInput.value <= startInput.value) {
            formError.textContent =
                "종료 일시는 시작 일시보다 늦어야 합니다.";
            return;
        }

        const body = {
            title: titleInput.value.trim(),
            content: contentInput.value,
            startAt: startInput.value,
            endAt: endInput.value,
            version: selectedVersion
        };

        const isUpdate = selectedId !== null;
        const url = isUpdate
            ? `${apiUrl}/${selectedId}`
            : apiUrl;

        saving = true;
        saveButton.disabled = true;
        closeButton.disabled = true;
        cancelButton.disabled = true;
        fields.disabled = true;
        saveButton.textContent = "저장 중...";

        try {
            await request(url, {
                method: isUpdate ? "PUT" : "POST",
                body: JSON.stringify(body)
            });

            dialog.close();

            // 저장한 일정의 시작 월로 이동
            viewDate = new Date(`${body.startAt.slice(0, 10)}T12:00:00`);
            viewDate.setDate(1);

            await loadCalendar(
                isUpdate
                    ? "일정을 수정했습니다."
                    : "일정을 등록했습니다."
            );
        } catch (error) {
            // 실패한 경우 입력 내용은 그대로 유지
            formError.textContent = error.message;
        } finally {
            saving = false;
            saveButton.disabled = false;
            closeButton.disabled = false;
            cancelButton.disabled = false;
            fields.disabled = !editable;
            saveButton.textContent = isUpdate ? "수정 저장" : "등록";
        }
    });

    // 취소는 폼 검증과 별개로 처리: 기존 입력을 고치지 않고도 취소 가능.
    cancelButton.addEventListener("click", async () => {
        if (saving || !editable || selectedId === null) return;
        if (!window.confirm(
            "이 일정을 취소할까요? 해당 시간이 비워지고 취소 기록은 남습니다."
        )) return;

        saving = true;
        formError.textContent = "";
        saveButton.disabled = true;
        cancelButton.disabled = true;
        closeButton.disabled = true;
        fields.disabled = true;

        try {
            await request(`${apiUrl}/${selectedId}/cancel`, {
                method: "POST",
                body: JSON.stringify({ version: selectedVersion })
            });
            dialog.close();
            await loadCalendar("일정을 취소했습니다. 해당 시간에 다시 등록할 수 있습니다.");
        } catch (error) {
            formError.textContent = error.message;
        } finally {
            saving = false;
            saveButton.disabled = false;
            cancelButton.disabled = false;
            closeButton.disabled = false;
            fields.disabled = !editable;
        }
    });

    closeButton.addEventListener("click", () => {
        if (!saving) dialog.close();
    });

    // 저장 중에는 Esc로 닫지 못하도록 처리
    dialog.addEventListener("cancel", (event) => {
        if (saving) event.preventDefault();
    });

    $("addSchedule").addEventListener("click", () => {
        openCreate(koreanToday());
    });

    $("prevMonth").addEventListener("click", () => {
        viewDate.setMonth(viewDate.getMonth() - 1);
        void loadCalendar();
    });

    $("nextMonth").addEventListener("click", () => {
        viewDate.setMonth(viewDate.getMonth() + 1);
        void loadCalendar();
    });

    $("todayButton").addEventListener("click", () => {
        viewDate = new Date(`${koreanToday()}T12:00:00`);
        viewDate.setDate(1);
        void loadCalendar();
    });

    void loadCalendar();
})();
