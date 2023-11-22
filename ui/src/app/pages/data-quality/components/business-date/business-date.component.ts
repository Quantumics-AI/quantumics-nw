import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { RuleCreationService } from '../../services/rule-creation.service';


@Component({
  selector: 'app-business-date',
  templateUrl: './business-date.component.html',
  styleUrls: ['./business-date.component.scss']
})
export class BusinessDateComponent {

  @Input() userId: number;
  @Input() projectId: number;
  @Input() ruleIds: any;

  public selectedBusinessDate: NgbDateStruct;
  // selectedBusinessDate: NgbDateStruct;

  constructor(
    public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private ruleCreationService: RuleCreationService
  ) { }

  ngOnInit(): void {
  }

  public deleteSource(): void {
    this.modal.close();
  }

  getFormattedDate(): string {
    if (this.selectedBusinessDate) {
        const day = this.selectedBusinessDate.day.toString().padStart(2, '0');
        const month = this.selectedBusinessDate.month.toString().padStart(2, '0');
        const year = this.selectedBusinessDate.year;

        return `${day}-${month}-${year}`;
    }
    return '';
  }

  public runRule(): void {

    const formattedDate = this.getFormattedDate();
    const lastTwoDigitsOfYear = formattedDate.slice(-2);

    const request = {
      ruleIds : this.ruleIds.ruleIds,
      businessDate : `${formattedDate.substring(0, 6)}${lastTwoDigitsOfYear}`
    };

    console.log(request);

    this.ruleCreationService.runRule(this.userId, this.projectId, request).subscribe((response) => {
      // this.loading = false;
      // this.ruleId = [];
      this.modal.close(response);
      this.snakbar.open(response.message, '', 7000);
      
    }, (error) => {
      // this.loading = false;
    });
  }
}